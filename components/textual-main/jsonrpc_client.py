import asyncio
import subprocess
import time
from pathlib import Path
from simple_jsonrpc_client import SimpleJSONRPCClient


async def start_server_and_connect(port=51234, wait_time=2):
    """
    Start the Clojure JSON-RPC server and connect to it.
    
    Returns:
        tuple: (client, server_process) where client is SimpleJSONRPCClient
               and server_process is the subprocess.Popen object
    """
    # Get the playground-jsonrpc directory
    project_root = Path(__file__).parent.parent.parent
    playground_dir = project_root / "components" / "playground-jsonrpc"
    
    # Start the Clojure server
    server_cmd = [
        "clojure", "-M", "-m",
        "com.dx.playground-jsonrpc.simple-server",
        "--socket", "--port", str(port)
    ]
    
    print(f"Starting Clojure JSON-RPC server on port {port}...")
    server_process = subprocess.Popen(
        server_cmd,
        cwd=str(playground_dir),
        stderr=subprocess.PIPE,
        stdout=subprocess.PIPE
    )
    
    print(f"Server subprocess PID: {server_process.pid}")
    print(f"Waiting {wait_time} seconds for server to bind socket...")
    await asyncio.sleep(wait_time)
    
    # Check if server is still alive
    if server_process.poll() is not None:
        raise RuntimeError(f"Server exited with code {server_process.poll()}")
    
    # Connect to server
    print(f"Connecting to server on localhost:{port}...")
    reader, writer = await asyncio.open_connection("localhost", port)
    print("Connected!")
    
    client = SimpleJSONRPCClient(reader, writer)
    return client, server_process


async def main():
    """Test the client with greet method"""
    client, server_process = await start_server_and_connect()
    
    try:
        # Test greet method
        print("\n=== Test: greet method ===")
        print("Sending greet request with params=['John']...")
        result = await client.send_request("greet", params=["John"])
        print(f"Success! Result: {result}")
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
    finally:
        await client.close()
        # Terminate server process
        server_process.terminate()
        server_process.wait()
        print("\nServer process closed.")


if __name__ == "__main__":
    asyncio.run(main())
