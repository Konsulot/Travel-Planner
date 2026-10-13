import os
from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()

client = OpenAI(
    api_key=os.getenv("OPENAI_API_KEY"),
    base_url=os.getenv("OPENAI_BASE_URL"),
)
MODEL = os.getenv("OPENAI_MODEL")


def main():
    response = client.chat.completions.create(
        model=MODEL,
        messages=[{"role": "user", "content": "Что такое токен?"}],
    )
    choice = response.choices[0]
    print("content:", choice.message.content)
    print("finish_reason:", choice.finish_reason)
    print(f"usage: prompt={response.usage.prompt_tokens}, "
          f"completion={response.usage.completion_tokens}")


if __name__ == "__main__":
    main()