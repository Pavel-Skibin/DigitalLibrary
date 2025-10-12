/**
 * Форматирование даты в читаемый вид
 */
export function formatDate(dateString) {
    if (!dateString) return '';

    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/**
 * Форматирование названия роли
 */
export function formatRole(roleName) {
    const roleMap = {
        'ROLE_USER': 'Пользователь',
        'ROLE_MODERATOR': 'Модератор',
        'ROLE_ADMIN': 'Администратор',
        'USER': 'Пользователь',
        'MODERATOR': 'Модератор',
        'ADMIN': 'Администратор'
    };
    return roleMap[roleName] || roleName;
}

/**
 * Форматирование рейтинга
 */
export function formatRating(rating) {
    if (rating == null) return '0.00';
    return Number(rating).toFixed(2);
}

/**
 * Форматирование процентов
 */
export function formatPercentage(value) {
    if (value == null) return '0.0%';
    return `${Number(value).toFixed(1)}%`;
}