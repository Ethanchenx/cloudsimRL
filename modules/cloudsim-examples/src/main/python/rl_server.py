import socket
import json
import numpy as np

def choose_action(state):
    return int(np.argmin(state))  # 示例：选择最空闲的 VM

def run_server():
    s = socket.socket()
    s.bind(("localhost", 5555))
    s.listen(1)
    print("✅ RL Server ready on port 5555")

    conn, addr = s.accept()
    print("🚀 Connected:", addr)

    with conn:
        while True:
            try:
                data = conn.recv(4096).decode().strip()
                if not data:
                    break

                req = json.loads(data)
                state = req['state']
                action = choose_action(state)

                print(f"[State] {state} → [Action] {action}")
                conn.sendall((json.dumps({"action": action}) + "\n").encode())

            except ConnectionResetError as e:
                print("❌ Connection reset by client. Exiting.")
                break  # 退出循环，避免无限等待

            except Exception as e:
                print(f"⚠️ Error: {e}")
                break

    s.close()
    print("✅ RL Server connection closed.")

if __name__ == "__main__":
    run_server()
