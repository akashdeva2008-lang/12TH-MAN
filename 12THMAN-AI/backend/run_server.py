"""
Run script for 12THMAN GPT-1 FastAPI Service.
Usage:
    python 12THMAN-AI/backend/run_server.py [--host 0.0.0.0] [--port 8000]
"""

import sys
import os
import argparse
import uvicorn

backend_dir = os.path.dirname(os.path.abspath(__file__))
if backend_dir not in sys.path:
    sys.path.insert(0, backend_dir)

def main():
    parser = argparse.ArgumentParser(description="12THMAN GPT-1 AI Service Runner")
    parser.add_argument("--host", type=str, default="0.0.0.0", help="Host interface (default: 0.0.0.0)")
    parser.add_argument("--port", type=int, default=8000, help="Port (default: 8000)")
    args = parser.parse_args()

    print(f"=======================================================")
    print(f"       12THMAN GPT-1 CRICKET AI SERVICE STARTUP")
    print(f"       Host: {args.host} | Port: {args.port}")
    print(f"       Endpoints:")
    print(f"         - GET  http://{args.host}:{args.port}/health")
    print(f"         - GET  http://{args.host}:{args.port}/model-info")
    print(f"         - POST http://{args.host}:{args.port}/chat")
    print(f"         - POST http://{args.host}:{args.port}/generate")
    print(f"=======================================================")

    uvicorn.run("main:app", host=args.host, port=args.port, reload=False)

if __name__ == "__main__":
    main()
