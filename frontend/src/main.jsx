import React, { useEffect, useMemo, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  LogIn,
  LogOut,
  Radio,
  RotateCcw,
  UserPlus,
} from "lucide-react";
import "./styles.css";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

const missionSteps = [
  { key: "MISSION_CREATED", label: "미션 생성" },
  { key: "ROBOT_ASSIGNED", label: "로봇 배정" },
  { key: "MISSION_STARTED", label: "로봇 출발" },
  { key: "MISSION_ARRIVED", label: "목적지 도착" },
  { key: "MISSION_RETURN_STARTED", label: "복귀 시작" },
  { key: "MISSION_FINISHED", label: "미션 완료" },
];

const eventLabels = {
  MISSION_CREATED: "미션 생성",
  ROBOT_ASSIGNED: "로봇 배정 완료",
  MISSION_STARTED: "로봇 출발",
  MISSION_ARRIVED: "목적지 도착",
  MISSION_RETURN_STARTED: "복귀 시작",
  MISSION_FINISHED: "미션 완료",
  MISSION_FAILED: "미션 실패",
};

function App() {
  const [view, setView] = useState("login");
  const [token, setToken] = useState(() => localStorage.getItem("accessToken") || "");
  const [expiresAt, setExpiresAt] = useState(() => localStorage.getItem("expiresAt") || "");
  const [username, setUsername] = useState(() => localStorage.getItem("username") || "");
  const [missionId, setMissionId] = useState(() => localStorage.getItem("missionId") || "");
  const [missionStatus, setMissionStatus] = useState("READY");
  const [events, setEvents] = useState([]);
  const [sseStatus, setSseStatus] = useState("DISCONNECTED");
  const [errorMessage, setErrorMessage] = useState("");
  const [showCompletionNotice, setShowCompletionNotice] = useState(false);
  const [showSignupSuccess, setShowSignupSuccess] = useState(false);
  const abortControllerRef = useRef(null);

  const isLoggedIn = Boolean(token);

  useEffect(() => {
    if (!token) {
      return;
    }

    setView("dashboard");
    connectSse(token);

    return () => {
      abortControllerRef.current?.abort();
    };
  }, [token]);

  const activeStepIndex = useMemo(() => {
    const index = missionSteps.findIndex((step) => step.key === missionStatus);
    return index < 0 ? -1 : index;
  }, [missionStatus]);

  async function request(path, options = {}) {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    });

    const responseText = await response.text();
    const body = responseText ? JSON.parse(responseText) : null;

    if (!response.ok) {
      throw new Error(body?.message || "요청 처리에 실패했습니다.");
    }

    return body;
  }

  async function handleSignup(form) {
    setErrorMessage("");

    await request("/users", {
      method: "POST",
      body: JSON.stringify(form),
      headers: {},
    });

    setShowSignupSuccess(true);
  }

  async function handleLogin(form) {
    setErrorMessage("");
    setShowSignupSuccess(false);

    const body = await request("/auth/login", {
      method: "POST",
      body: JSON.stringify(form),
      headers: {},
    });

    const accessToken = body.data.accessToken;
    const refreshToken = body.data.refreshToken;
    localStorage.setItem("accessToken", accessToken);
    localStorage.setItem("refreshToken", refreshToken);
    localStorage.setItem("expiresAt", body.data.expiresAt);
    localStorage.setItem("username", form.username);

    setToken(accessToken);
    setExpiresAt(body.data.expiresAt);
    setUsername(form.username);
  }

  async function createMission() {
    setErrorMessage("");
    setShowCompletionNotice(false);

    const body = await request("/missions", {
      method: "POST",
      body: JSON.stringify({}),
    });

    const nextMissionId = String(body.data.missionId);
    localStorage.setItem("missionId", nextMissionId);
    setMissionId(nextMissionId);
    setMissionStatus("MISSION_CREATED");
    appendEvent({
      eventType: "MISSION_CREATED",
      missionId: body.data.missionId,
      message: "미션 생성 요청이 접수되었습니다.",
    });
  }

  async function returnRobot() {
    if (!missionId) {
      setErrorMessage("복귀시킬 미션이 없습니다.");
      return;
    }

    setErrorMessage("");
    await request(`/missions/${missionId}/return`, {
      method: "POST",
      body: JSON.stringify({}),
    });
  }

  async function logout() {
    try {
      if (token) {
        await request("/auth/logout", {
          method: "POST",
          body: JSON.stringify({}),
        });
      }
    } catch {
      // 서버 로그아웃 요청이 실패해도 현재 브라우저의 인증 정보는 정리한다.
    } finally {
      abortControllerRef.current?.abort();
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("expiresAt");
      localStorage.removeItem("username");
      localStorage.removeItem("missionId");
      setToken("");
      setExpiresAt("");
      setUsername("");
      setMissionId("");
      setMissionStatus("READY");
      setEvents([]);
      setSseStatus("DISCONNECTED");
      setShowCompletionNotice(false);
      setView("login");
    }
  }

  function reconnectAfterCompletion() {
    setShowCompletionNotice(false);

    if (!token) {
      return;
    }

    connectSse(token);
  }

  function confirmSignupSuccess() {
    setShowSignupSuccess(false);
    setErrorMessage("");
    setView("login");
  }

  async function connectSse(accessToken) {
    abortControllerRef.current?.abort();
    const controller = new AbortController();
    abortControllerRef.current = controller;

    try {
      setSseStatus("CONNECTING");
      const response = await fetch(`${API_BASE_URL}/notifications/subscribe`, {
        headers: {
          Accept: "text/event-stream",
          Authorization: `Bearer ${accessToken}`,
        },
        signal: controller.signal,
      });

      if (!response.ok || !response.body) {
        throw new Error("실시간 연결에 실패했습니다.");
      }

      setSseStatus("CONNECTED");
      await readSseStream(response.body, controller.signal);
    } catch (error) {
      if (error.name === "AbortError") {
        return;
      }

      setSseStatus("DISCONNECTED");
      setErrorMessage(error.message);
    }
  }

  async function readSseStream(stream, signal) {
    const reader = stream.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";

    while (!signal.aborted) {
      const { value, done } = await reader.read();

      if (done) {
        setSseStatus("DISCONNECTED");
        break;
      }

      buffer += decoder.decode(value, { stream: true });
      const chunks = buffer.split("\n\n");
      buffer = chunks.pop() || "";

      chunks.forEach(handleSseChunk);
    }
  }

  function handleSseChunk(chunk) {
    const lines = chunk.split("\n");
    const eventName = lines.find((line) => line.startsWith("event:"))?.replace("event:", "").trim();
    const data = lines
      .filter((line) => line.startsWith("data:"))
      .map((line) => line.replace("data:", "").trim())
      .join("");

    if (!data) {
      return;
    }

    if (eventName === "connect") {
      localStorage.removeItem("missionId");
      setMissionId("");
      setMissionStatus("READY");
      return;
    }

    try {
      const payload = JSON.parse(data);

      appendEvent(payload);

      if (payload.eventType) {
        setMissionStatus(payload.eventType);
      }

      if (payload.eventType === "MISSION_FINISHED") {
        setShowCompletionNotice(true);
      }

      if (payload.missionId) {
        const nextMissionId = String(payload.missionId);
        localStorage.setItem("missionId", nextMissionId);
        setMissionId(nextMissionId);
      }
    } catch {
      appendEvent({
        eventType: eventName || "MESSAGE",
        message: data,
      });
    }
  }

  function appendEvent(payload) {
    setEvents((prevEvents) => [
      {
        id: `${Date.now()}-${Math.random()}`,
        receivedAt: new Date().toLocaleTimeString(),
        ...payload,
      },
      ...prevEvents,
    ]);
  }

  return (
    <main className="app-shell">
      <section className="brand-panel">
        <div className="brand-mark">
          <img className="brand-logo" src="/images/logo.png" alt="Carry Porter" />
        </div>
        <p className="eyebrow">Carry Porter Control Tower</p>
        <h1>CarryPorter MissionFlow Live</h1>
        <p className="brand-copy">
          사용자 호출부터 로봇 배정, 출발, 도착, 복귀, 완료까지 refactoring 서버의 이벤트 흐름을 실시간으로 확인합니다.
        </p>
        <div className="signal-row">
          <span className={`signal-dot ${sseStatus.toLowerCase()}`} />
          <span>{isLoggedIn ? `SSE ${sseStatus}` : "로그인 후 실시간 연결"}</span>
        </div>
      </section>

      {view === "login" && (
        <AuthPanel
          mode="login"
          title="로그인"
          icon={<LogIn size={18} />}
          buttonText="로그인"
          errorMessage={errorMessage}
          onSubmit={handleLogin}
          onSwitch={() => {
            setErrorMessage("");
            setShowSignupSuccess(false);
            setView("signup");
          }}
        />
      )}

      {view === "signup" && (
        <AuthPanel
          mode="signup"
          title="회원가입"
          icon={<UserPlus size={18} />}
          buttonText="회원가입"
          errorMessage={errorMessage}
          showSignupSuccess={showSignupSuccess}
          onConfirmSignupSuccess={confirmSignupSuccess}
          onSubmit={handleSignup}
          onSwitch={() => {
            setErrorMessage("");
            setShowSignupSuccess(false);
            setView("login");
          }}
        />
      )}

      {view === "dashboard" && (
        <Dashboard
          username={username}
          expiresAt={expiresAt}
          missionId={missionId}
          missionStatus={missionStatus}
          activeStepIndex={activeStepIndex}
          events={events}
          errorMessage={errorMessage}
          onCreateMission={createMission}
          onReturnRobot={returnRobot}
          onLogout={logout}
          showCompletionNotice={showCompletionNotice}
          onConfirmCompletion={reconnectAfterCompletion}
        />
      )}
    </main>
  );
}

function AuthPanel({
  mode,
  title,
  icon,
  buttonText,
  errorMessage,
  showSignupSuccess,
  onConfirmSignupSuccess,
  onSubmit,
  onSwitch,
}) {
  const [form, setForm] = useState({ username: "", password: "" });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [localMessage, setLocalMessage] = useState("");
  const switchText = mode === "login" ? "회원가입" : "로그인";

  async function submit(event) {
    event.preventDefault();
    setIsSubmitting(true);
    setLocalMessage("");

    try {
      await onSubmit(form);
    } catch (error) {
      setLocalMessage(error.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="auth-panel">
      <div className="panel-header">
        <span className="panel-icon">{icon}</span>
        <h2>{title}</h2>
      </div>

      {showSignupSuccess && (
        <section className="auth-success-notice" aria-live="polite">
          <div>
            <span className="completion-kicker">Signup Complete</span>
            <strong>회원가입이 완료되었습니다.</strong>
            <p>확인을 누르면 로그인 화면으로 이동합니다.</p>
          </div>
          <button className="notice-button" type="button" onClick={onConfirmSignupSuccess}>
            확인
          </button>
        </section>
      )}

      <form onSubmit={submit} className="auth-form">
        <label>
          <span>Username</span>
          <input
            value={form.username}
            onChange={(event) => setForm({ ...form, username: event.target.value })}
            placeholder="username"
            autoComplete="username"
          />
        </label>

        <label>
          <span>Password</span>
          <input
            value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })}
            placeholder="8자 이상"
            type="password"
            autoComplete={mode === "login" ? "current-password" : "new-password"}
          />
        </label>

        <p className="feedback" data-auth-error>
          {localMessage || errorMessage}
        </p>

        <button className="primary-button" type="submit" disabled={isSubmitting}>
          {icon}
          {isSubmitting ? "처리 중" : buttonText}
        </button>
      </form>

      <button className="text-button" type="button" onClick={onSwitch}>
        {switchText}
      </button>
    </section>
  );
}

function Dashboard({
  username,
  expiresAt,
  missionId,
  missionStatus,
  activeStepIndex,
  events,
  errorMessage,
  onCreateMission,
  onReturnRobot,
  onLogout,
  showCompletionNotice,
  onConfirmCompletion,
}) {
  const isReturnEnabled = missionStatus === "MISSION_ARRIVED";

  return (
    <section className="dashboard">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">Operator Console</p>
          <h2>{username}</h2>
          <p className="token-copy">Token expires at {expiresAt || "-"}</p>
        </div>
        <button className="icon-button" type="button" onClick={onLogout} aria-label="logout">
          <LogOut size={18} />
        </button>
      </header>

      <div className="mission-strip">
        <div>
          <span>Mission</span>
          <strong>{missionId || "대기 중"}</strong>
        </div>
        <div>
          <span>Status</span>
          <strong>{missionStatus}</strong>
        </div>
      </div>

      <div className="action-row">
        <button className="primary-button" type="button" onClick={onCreateMission}>
          <Activity size={18} />
          로봇 호출
        </button>
        <button className="secondary-button" type="button" onClick={onReturnRobot} disabled={!isReturnEnabled}>
          <RotateCcw size={18} />
          복귀
        </button>
      </div>

      {errorMessage && (
        <p className="error-line">
          <AlertTriangle size={16} />
          {errorMessage}
        </p>
      )}

      {showCompletionNotice && (
        <section className="completion-notice" aria-live="polite">
          <div>
            <span className="completion-kicker">Mission Complete</span>
            <strong>미션이 정상 종료되었습니다.</strong>
            <p>확인하면 SSE 연결을 다시 동기화해서 다음 호출을 준비합니다.</p>
          </div>
          <button className="notice-button" type="button" onClick={onConfirmCompletion}>
            확인
          </button>
        </section>
      )}

      <ol className="step-list">
        {missionSteps.map((step, index) => (
          <li
            key={step.key}
            className={[
              index <= activeStepIndex ? "done" : "",
              step.key === missionStatus ? "current" : "",
            ].join(" ")}
          >
            <CheckCircle2 size={16} />
            <span>{step.label}</span>
          </li>
        ))}
      </ol>

      <section className="event-panel">
        <div className="panel-header compact">
          <span className="panel-icon">
            <Radio size={17} />
          </span>
          <h3>Live Mission Feed</h3>
        </div>

        <div className="event-list">
          {events.length === 0 && <p className="empty-state">아직 수신된 이벤트가 없습니다.</p>}

          {events.map((event) => (
            <article className="event-item" key={event.id}>
              <span className="event-time">{event.receivedAt}</span>
              <strong>{eventLabels[event.eventType] || event.eventType}</strong>
              <p>{event.message}</p>
              {event.failureCode && <code>{event.failureCode}</code>}
            </article>
          ))}
        </div>
      </section>
    </section>
  );
}

createRoot(document.getElementById("root")).render(<App />);
