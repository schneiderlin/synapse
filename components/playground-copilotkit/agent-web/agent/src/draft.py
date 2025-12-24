import asyncio

from pydantic_ai.ag_ui import StateDeps
from agent import ProverbsState, agent

async def main():
    result = await agent.run("hello", deps=StateDeps(ProverbsState()))
    print(result)

if __name__ == "__main__":
    asyncio.run(main())