/**
 * Получить значение cookie по имени
 */
export function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(";").shift();
  return null;
}

/**
 * Установить cookie с защитой от CSRF
 * @param {string} name - Имя cookie
 * @param {string} value - Значение cookie
 * @param {number} days - Количество дней до истечения (по умолчанию 7)
 * @param {Object} options - Дополнительные опции (secure, sameSite)
 */
export function setCookie(name, value, days = 7, options = {}) {
  const expires = new Date();
  expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);

  const {
    secure = false, // true только для HTTPS
    sameSite = "Lax", // 'Strict', 'Lax', или 'None'
  } = options;

  let cookieString = `${name}=${value};expires=${expires.toUTCString()};path=/;SameSite=${sameSite}`;

  if (secure) {
    cookieString += ";Secure";
  }

  document.cookie = cookieString;
}

/**
 * Удалить cookie
 */
export function deleteCookie(name) {
  document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/;SameSite=Lax`;
}
