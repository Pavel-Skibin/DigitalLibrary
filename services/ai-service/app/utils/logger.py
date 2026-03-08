import sys
from loguru import logger
from app.config import settings


def setup_logger():
    """Configure loguru logger"""
    logger.remove()  # Remove default handler
    
    # Console logger
    logger.add(
        sys.stdout,
        format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>",
        level=settings.LOG_LEVEL,
        colorize=True
    )
    
    # File logger (errors only)
    logger.add(
        "logs/errors.log",
        format="{time:YYYY-MM-DD HH:mm:ss} | {level} | {name}:{function}:{line} - {message}",
        level="ERROR",
        rotation="10 MB",
        retention="30 days",
        compression="zip"
    )
    
    # File logger (all logs)
    logger.add(
        "logs/ai-service.log",
        format="{time:YYYY-MM-DD HH:mm:ss} | {level} | {name}:{function}:{line} - {message}",
        level="DEBUG" if settings.DEBUG else "INFO",
        rotation="50 MB",
        retention="7 days",
        compression="zip"
    )
    
    logger.info("Logger initialized")


# Initialize logger when module is imported
setup_logger()
