#!/usr/bin/env python3
"""
Simple JSON-RPC client using asyncio streams.
"""
import asyncio
import json


class SimpleJSONRPCClient:
    """Simple JSON-RPC client using asyncio streams"""
    
    def __init__(self, reader, writer):
        self.reader = reader
        self.writer = writer
        self.request_id = 0
    
    async def send_request(self, method, params=None):
        """Send a JSON-RPC request and wait for response"""
        self.request_id += 1
        request_id = self.request_id
        
        request = {
            "jsonrpc": "2.0",
            "method": method,
            "id": request_id
        }
        
        if params is not None:
            request["params"] = params
        
        # Format as Content-Length message (like LSP)
        request_json = json.dumps(request)
        request_bytes = request_json.encode('utf-8')
        content_length = len(request_bytes)
        header = f"Content-Length: {content_length}\r\n\r\n"
        message = header.encode('utf-8') + request_bytes
        
        # Send request
        self.writer.write(message)
        await self.writer.drain()
        
        # Read response
        # First read the Content-Length header
        header_lines = []
        while True:
            line = await self.reader.readline()
            if not line:
                raise ConnectionError("Connection closed")
            line_str = line.decode('utf-8').strip()
            if not line_str:
                break  # Empty line means end of headers
            header_lines.append(line_str)
        
        # Parse Content-Length
        content_length = None
        for line in header_lines:
            if line.startswith('Content-Length:'):
                content_length = int(line.split(':', 1)[1].strip())
                break
        
        if content_length is None:
            raise ValueError("No Content-Length header in response")
        
        # Read the message body
        response_bytes = await self.reader.readexactly(content_length)
        response_json = response_bytes.decode('utf-8')
        response = json.loads(response_json)
        
        # Check for errors
        if "error" in response:
            error = response["error"]
            raise Exception(f"JSON-RPC error: {error.get('message', 'Unknown error')} (code: {error.get('code', 'unknown')})")
        
        return response.get("result")
    
    async def close(self):
        """Close the connection"""
        self.writer.close()
        await self.writer.wait_closed()

