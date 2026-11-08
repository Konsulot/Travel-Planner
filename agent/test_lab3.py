"""Проверки лаб. 3 без обращения к модели: python test_lab3.py
Ключ API не нужен — запросы к модели не отправляются."""
import os

os.environ.setdefault("OPENAI_API_KEY", "offline-test")  # клиент создаётся, но не используется

import agent


def call(i, name, args="{}"):
    return {"role": "assistant", "content": "",
            "tool_calls": [{"id": f"c{i}", "type": "function",
                            "function": {"name": name, "arguments": args}}]}


def tool(i, text):
    return {"role": "tool", "tool_call_id": f"c{i}", "content": text}


def check_valid(messages):
    """Каждый role=tool должен идти сразу после assistant с его tool_call_id."""
    pending = set()
    for m in messages:
        if m["role"] == "tool":
            assert m["tool_call_id"] in pending, f"осиротевший tool: {m['tool_call_id']}"
            pending.discard(m["tool_call_id"])
        else:
            assert not pending, f"tool-цикл не закрыт: {pending}"
            pending = {c["id"] for c in m.get("tool_calls") or []}


# 1. Усечение: история из 5 ходов, маленький бюджет
big = "x" * 2000
history = [{"role": "system", "content": agent.SYSTEM_PROMPT}]
for i in range(5):
    history += [{"role": "user", "content": f"вопрос {i}"},
                call(i, "read_file", '{"path": "README.md"}'), tool(i, big),
                {"role": "assistant", "content": f"ответ {i}"}]

agent.STRATEGY = "trim"
agent.CONTEXT_BUDGET = agent.estimate_tokens(history[:1]) + 1200
before = len(history)
agent.ensure_context_budget(history)
check_valid(history)
assert history[0]["role"] == "system", "системный промпт потерян"
assert history[-1]["content"] == "ответ 4", "последний ход потерян"
print(f"OK trim: {before} -> {len(history)} сообщений, история валидна")

# 2. Текущий ход сам больше бюджета -> сокращаются старые результаты инструментов
turn = [{"role": "system", "content": agent.SYSTEM_PROMPT}, {"role": "user", "content": "прочитай всё"},
        call(1, "read_file"), tool(1, big), call(2, "read_file"), tool(2, big)]
agent.CONTEXT_BUDGET = agent.estimate_tokens(turn) - 100
agent.ensure_context_budget(turn)
check_valid(turn)
assert agent.SHORTENED_MARK in turn[3]["content"], "старый результат не сокращён"
assert turn[5]["content"] == big, "свежий результат (модель его ещё не видела) трогать нельзя"
print("OK shorten: старый результат сокращён, свежий цел")

# 3. Починка истории после аварийного выхода
broken = [{"role": "user", "content": "1"}, call(1, "list_files"), tool(1, "ok"),
          {"role": "assistant", "content": "готово"}, {"role": "user", "content": "2"},
          call(2, "list_files")]  # программу закрыли до ответа инструмента
fixed = agent.repair_history(broken)
check_valid(fixed)
assert [m["role"] for m in fixed] == ["user", "assistant", "tool", "assistant", "user"]
print("OK repair_history: незавершённый tool-цикл отброшен")

# 4. id сессии не выводит за пределы sessions/
for bad in ["../secret", "a/b", ""]:
    try:
        agent.session_file(bad)
        raise AssertionError(f"id '{bad}' не отклонён")
    except ValueError:
        pass
print("OK session_file: опасные id отклоняются")

# 5. search_docs
print(agent.execute_tool("search_docs", '{"query": "бюджет демо-поездки", "limit": 1}'))
assert "faq.md" in agent.execute_tool("search_docs", '{"query": "бюджет демо-поездки", "limit": 1}')
assert agent.execute_tool("search_docs", '{"query": ""}').startswith("ОШИБКА")
assert agent.execute_tool("search_docs", '{"query": "бюджет", "limit": 9}').startswith("ОШИБКА")
assert "ничего не найдено" in agent.execute_tool("search_docs", '{"query": "квантовая физика"}')
print("OK search_docs: поиск, ошибки и пустой результат")

print("\nВсе проверки пройдены.")
