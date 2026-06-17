import json
import logging
import os
import threading
import time
from dataclasses import dataclass
from enum import Enum

import paho.mqtt.client as mqtt


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)

logger = logging.getLogger(__name__)

BROKER_HOST = os.getenv("MQTT_BROKER_HOST", "localhost")
BROKER_PORT = int(os.getenv("MQTT_BROKER_PORT", "1883"))
MQTT_USERNAME = os.getenv("MQTT_USERNAME", "")
MQTT_PASSWORD = os.getenv("MQTT_PASSWORD", "")
ROBOT_MAC_ADDRESS = os.getenv("ROBOT_MAC_ADDRESS", "AA:BB:CC:DD:EE:01")
MQTT_QOS = int(os.getenv("MQTT_QOS", "1"))
SIMULATED_TRAVEL_SECONDS = int(os.getenv("SIMULATED_TRAVEL_SECONDS", "5"))


class RobotSimulationState(Enum):
    IDLE = "IDLE"
    MOVING_TO_DESTINATION = "MOVING_TO_DESTINATION"
    ARRIVED = "ARRIVED"
    RETURNING = "RETURNING"


@dataclass
class MissionContext:
    mission_id: int
    user_id: int


state_lock = threading.Lock()
robot_state = RobotSimulationState.IDLE
current_mission_context: MissionContext | None = None


def build_command_topic(mac_address: str) -> str:
    return f"v1/robots/{mac_address}/command/#"


def build_connected_topic(mac_address: str) -> str:
    return f"v1/robots/{mac_address}/event/connected"


def build_disconnected_topic(mac_address: str) -> str:
    return f"v1/robots/{mac_address}/event/disconnected"


def build_arrived_topic(mac_address: str) -> str:
    return f"v1/robots/{mac_address}/event/arrived"


def build_returned_topic(mac_address: str) -> str:
    return f"v1/robots/{mac_address}/event/returned"


def parse_command_name(topic: str) -> str | None:
    topic_tokens = topic.split("/")

    if len(topic_tokens) < 5:
        return None

    return topic_tokens[4]


def parse_command_payload(payload: str) -> dict | None:
    try:
        data = json.loads(payload)
    except json.JSONDecodeError:
        logger.error("payload JSON 파싱 실패: %s", payload)
        return None

    mission_id = data.get("missionId")
    user_id = data.get("userId")

    if mission_id is None or user_id is None:
        logger.error("missionId 또는 userId 누락: %s", payload)
        return None

    return {
        "missionId": mission_id,
        "userId": user_id,
    }


def publish_connected(client: mqtt.Client) -> None:
    topic = build_connected_topic(ROBOT_MAC_ADDRESS)
    client.publish(topic, payload="", qos=MQTT_QOS)
    logger.info("connected 이벤트 발행: topic=%s", topic)


def simulate_move_and_publish(
        client: mqtt.Client,
        event_topic: str,
        payload: dict,
        action_name: str,
) -> None:
    global robot_state
    global current_mission_context

    logger.info("%s 명령 수신. %s초 동안 이동 시뮬레이션 시작", action_name, SIMULATED_TRAVEL_SECONDS)
    time.sleep(SIMULATED_TRAVEL_SECONDS)

    message = json.dumps(payload, separators=(",", ":"))
    client.publish(event_topic, payload=message, qos=MQTT_QOS)
    logger.info("%s 이벤트 발행 완료: topic=%s payload=%s", action_name, event_topic, message)

    with state_lock:
        if action_name == "departure":
            robot_state = RobotSimulationState.ARRIVED
            logger.info("robot 상태 변경: ARRIVED")
            return

        if action_name == "return":
            robot_state = RobotSimulationState.IDLE
            current_mission_context = None
            logger.info("robot 상태 변경: IDLE")


def handle_departure(client: mqtt.Client, payload: dict) -> None:
    global robot_state
    global current_mission_context

    requested_mission = MissionContext(
        mission_id=payload["missionId"],
        user_id=payload["userId"],
    )

    with state_lock:
        if robot_state == RobotSimulationState.MOVING_TO_DESTINATION and current_mission_context == requested_mission:
            logger.warning("중복 departure 명령 무시: missionId=%s", requested_mission.mission_id)
            return

        if robot_state in (RobotSimulationState.MOVING_TO_DESTINATION, RobotSimulationState.RETURNING):
            logger.warning(
                "진행 중인 작업이 있어 departure 명령 무시: state=%s currentMissionId=%s requestedMissionId=%s",
                robot_state.value,
                current_mission_context.mission_id if current_mission_context else None,
                requested_mission.mission_id,
            )
            return

        if robot_state == RobotSimulationState.ARRIVED and current_mission_context == requested_mission:
            logger.warning("이미 목적지 도착 처리된 mission 의 departure 명령 무시: missionId=%s", requested_mission.mission_id)
            return

        if robot_state == RobotSimulationState.ARRIVED and current_mission_context != requested_mission:
            logger.warning(
                "아직 복귀 전 상태라 다른 mission 의 departure 명령 무시: currentMissionId=%s requestedMissionId=%s",
                current_mission_context.mission_id if current_mission_context else None,
                requested_mission.mission_id,
            )
            return

        current_mission_context = requested_mission
        robot_state = RobotSimulationState.MOVING_TO_DESTINATION
        logger.info("robot 상태 변경: MOVING_TO_DESTINATION missionId=%s", requested_mission.mission_id)

    thread = threading.Thread(
        target=simulate_move_and_publish,
        args=(client, build_arrived_topic(ROBOT_MAC_ADDRESS), payload, "departure"),
        daemon=True,
    )
    thread.start()


def handle_return(client: mqtt.Client, payload: dict) -> None:
    global robot_state

    requested_mission = MissionContext(
        mission_id=payload["missionId"],
        user_id=payload["userId"],
    )

    with state_lock:
        if robot_state == RobotSimulationState.RETURNING and current_mission_context == requested_mission:
            logger.warning("중복 return 명령 무시: missionId=%s", requested_mission.mission_id)
            return

        if robot_state == RobotSimulationState.IDLE:
            logger.warning("대기 상태에서는 return 명령을 처리할 수 없어 무시: missionId=%s", requested_mission.mission_id)
            return

        if robot_state == RobotSimulationState.MOVING_TO_DESTINATION:
            logger.warning("아직 목적지 도착 전이므로 return 명령 무시: missionId=%s", requested_mission.mission_id)
            return

        if current_mission_context != requested_mission:
            logger.warning(
                "현재 수행 중인 mission 과 다른 return 명령 무시: currentMissionId=%s requestedMissionId=%s",
                current_mission_context.mission_id if current_mission_context else None,
                requested_mission.mission_id,
            )
            return

        robot_state = RobotSimulationState.RETURNING
        logger.info("robot 상태 변경: RETURNING missionId=%s", requested_mission.mission_id)

    thread = threading.Thread(
        target=simulate_move_and_publish,
        args=(client, build_returned_topic(ROBOT_MAC_ADDRESS), payload, "return"),
        daemon=True,
    )
    thread.start()


def on_connect(client: mqtt.Client, _userdata, _flags, reason_code, _properties=None) -> None:
    logger.info("MQTT broker 연결 완료: reasonCode=%s", reason_code)

    command_topic = build_command_topic(ROBOT_MAC_ADDRESS)
    client.subscribe(command_topic, qos=MQTT_QOS)
    logger.info("command topic 구독 완료: topic=%s", command_topic)

    publish_connected(client)


def on_disconnect(client: mqtt.Client, _userdata, disconnect_flags, reason_code, _properties=None) -> None:
    logger.info(
        "MQTT broker 연결 종료: reasonCode=%s disconnectFlags=%s",
        reason_code,
        disconnect_flags,
    )


def on_message(client: mqtt.Client, _userdata, message: mqtt.MQTTMessage) -> None:
    topic = message.topic
    payload_text = message.payload.decode("utf-8")

    logger.info("command 수신: topic=%s payload=%s", topic, payload_text)

    command_name = parse_command_name(topic)
    if command_name is None:
        logger.warning("지원하지 않는 topic 형식입니다: %s", topic)
        return

    payload = parse_command_payload(payload_text)
    if payload is None:
        return

    if command_name == "departure":
        handle_departure(client, payload)
        return

    if command_name == "return":
        handle_return(client, payload)
        return

    logger.warning("지원하지 않는 command 입니다: %s", command_name)


def create_client() -> mqtt.Client:
    client_id = f"robot-simulator-{ROBOT_MAC_ADDRESS}"
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=client_id)

    if MQTT_USERNAME:
        client.username_pw_set(MQTT_USERNAME, MQTT_PASSWORD)

    # 비정상 종료 시 broker 가 대신 disconnected 이벤트를 발행하도록 유언 메시지를 등록한다.
    client.will_set(
        topic=build_disconnected_topic(ROBOT_MAC_ADDRESS),
        payload="",
        qos=MQTT_QOS,
        retain=False,
    )

    client.on_connect = on_connect
    client.on_disconnect = on_disconnect
    client.on_message = on_message
    return client


def main() -> None:
    logger.info(
        "robot simulator 시작: broker=%s:%s macAddress=%s qos=%s travelSeconds=%s",
        BROKER_HOST,
        BROKER_PORT,
        ROBOT_MAC_ADDRESS,
        MQTT_QOS,
        SIMULATED_TRAVEL_SECONDS,
    )

    client = create_client()
    client.connect(BROKER_HOST, BROKER_PORT, keepalive=60)
    client.loop_forever()


if __name__ == "__main__":
    main()
