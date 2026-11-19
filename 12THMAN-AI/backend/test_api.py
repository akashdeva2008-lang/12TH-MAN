"""
Unit & Integration Test for 12THMAN GPT-1 FastAPI Backend.
Uses starlette.testclient to test endpoints without spinning up a separate socket.
"""

import sys
import os

backend_dir = os.path.dirname(os.path.abspath(__file__))
if backend_dir not in sys.path:
    sys.path.insert(0, backend_dir)

from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def test_endpoints():
    print("Testing GET / ...")
    r = client.get("/")
    assert r.status_code == 200, f"Root endpoint failed: {r.text}"
    print("  -> Response:", r.json())

    print("\nTesting GET /health ...")
    r = client.get("/health")
    assert r.status_code == 200, f"Health endpoint failed: {r.text}"
    data = r.json()
    assert data["model_loaded"] is True
    print("  -> Health:", data)

    print("\nTesting GET /model-info ...")
    r = client.get("/model-info")
    assert r.status_code == 200, f"Model info endpoint failed: {r.text}"
    info = r.json()
    assert info["pretrained_weights"] is False
    assert info["parameters"] > 0
    print(f"  -> Model Info: {info['name']} | Params: {info['parameters']:,} | Layers: {info['n_layer']} | Loss: {info['val_loss']:.4f}")

    print("\nTesting POST /chat ...")
    chat_payload = {
        "prompt": "What is an LBW?",
        "temperature": 0.7,
        "max_tokens": 40
    }
    r = client.post("/chat", json=chat_payload)
    assert r.status_code == 200, f"Chat failed: {r.text}"
    chat_res = r.json()
    print("  -> Chat Response:", chat_res["response"])
    print(f"  -> Tokens: {chat_res['tokens_generated']}, Latency: {chat_res['latency_ms']} ms")

    print("\nTesting POST /generate ...")
    gen_payload = {
        "prompt": "A yorker is",
        "max_tokens": 30,
        "temperature": 0.7
    }
    r = client.post("/generate", json=gen_payload)
    assert r.status_code == 200, f"Generate failed: {r.text}"
    gen_res = r.json()
    print("  -> Generate Result:", gen_res["generated_text"])

    print("\nAll Backend Endpoints Passed Successfully!")

if __name__ == "__main__":
    test_endpoints()
