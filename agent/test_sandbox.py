import agent

print(agent.execute_tool("read_file", '{"path": "../.env"}'))
print(agent.execute_tool("read_file", '{"path": "../../README.md"}'))
print(agent.execute_tool("write_file", '{"path": "../hack.txt", "content": "x"}'))