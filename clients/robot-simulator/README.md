# Robot Simulator

MQTT broker 에 연결해서 서버 명령을 받아 로봇 동작을 흉내 내는 간단한 시뮬레이터입니다.

## 동작

- 시작 시 `v1/robots/{mac-address}/event/connected` 발행
- 비정상 종료 시 broker 가 `v1/robots/{mac-address}/event/disconnected` 유언 메시지 발행
- `v1/robots/{mac-address}/command/departure` 수신 시 5초 뒤 `arrived` 발행
- `v1/robots/{mac-address}/command/return` 수신 시 5초 뒤 `returned` 발행

## 중복 명령 방어

- 동일한 미션에 대한 중복 `departure` 명령은 한 번만 처리
- 동일한 미션에 대한 중복 `return` 명령은 한 번만 처리
- 이동 중에는 다른 미션의 `departure` 명령 무시
- 목적지 도착 전 `return` 명령 무시
- 복귀 전에는 다른 미션의 `return` 명령 무시

## 준비

```bash
cd /Users/joonwan/project/ssafy/carry-porter-be-refactoring/clients/robot-simulator
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## 실행

```bash
cd /Users/joonwan/project/ssafy/carry-porter-be-refactoring/clients/robot-simulator
export MQTT_BROKER_HOST=localhost
export MQTT_BROKER_PORT=1883
export ROBOT_MAC_ADDRESS=AA:BB:CC:DD:EE:01
export MQTT_QOS=1
export SIMULATED_TRAVEL_SECONDS=5
python3 robot_client.py
```

## 환경 변수

- `MQTT_BROKER_HOST`: broker host, 기본값 `localhost`
- `MQTT_BROKER_PORT`: broker port, 기본값 `1883`
- `MQTT_USERNAME`: broker username, 기본값 빈 값
- `MQTT_PASSWORD`: broker password, 기본값 빈 값
- `ROBOT_MAC_ADDRESS`: 시뮬레이터가 사용할 MAC 주소
- `MQTT_QOS`: publish / subscribe QoS, 기본값 `1`
- `SIMULATED_TRAVEL_SECONDS`: 이동 시뮬레이션 시간, 기본값 `5`
