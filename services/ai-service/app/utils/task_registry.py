"""
TaskRegistry — in-memory хранилище фоновых задач.

Используется для отслеживания прогресса долгих операций:
- Генерация рекомендационных эмбеддингов
- RAG-индексация (чанкинг + эмбеддинг)

Жизненный цикл задачи: PENDING → RUNNING → DONE | ERROR
"""

from __future__ import annotations

import uuid
from dataclasses import dataclass, field
from enum import Enum
from typing import Optional, Dict

from loguru import logger


class TaskStatus(str, Enum):
    PENDING = "pending"
    RUNNING = "running"
    DONE    = "done"
    ERROR   = "error"


@dataclass
class Task:
    task_id:  str
    status:   TaskStatus       = TaskStatus.PENDING
    progress: int              = 0        # 0-100
    message:  str              = "Ожидание..."
    result:   Optional[dict]   = field(default=None)
    error:    Optional[str]    = field(default=None)


class TaskRegistry:
    """Простой in-memory реестр фоновых задач."""

    def __init__(self) -> None:
        self._tasks: Dict[str, Task] = {}

    def create(self) -> Task:
        task_id = str(uuid.uuid4())
        task = Task(task_id=task_id)
        self._tasks[task_id] = task
        logger.debug(f"Task created: {task_id}")
        return task

    def get(self, task_id: str) -> Optional[Task]:
        return self._tasks.get(task_id)

    def update(
        self,
        task_id: str,
        *,
        status:   Optional[TaskStatus] = None,
        progress: Optional[int]        = None,
        message:  Optional[str]        = None,
        result:   Optional[dict]       = None,
        error:    Optional[str]        = None,
    ) -> None:
        task = self._tasks.get(task_id)
        if task is None:
            return
        if status   is not None: task.status   = status
        if progress is not None: task.progress = progress
        if message  is not None: task.message  = message
        if result   is not None: task.result   = result
        if error    is not None: task.error    = error


# Singleton
task_registry = TaskRegistry()
