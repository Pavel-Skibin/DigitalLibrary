import { api } from './index';

/**
 * Получить список авторов с пагинацией
 */
export async function getAuthors(page = 0, size = 10, query = '') {
    const searchParam = query ? `&query=${encodeURIComponent(query)}` : '';
    return api.get(`/authors?page=${page}&size=${size}${searchParam}`);
}

/**
 * Создать автора
 */
export async function createAuthor(firstName, lastName) {
    const params = new URLSearchParams();
    params.append('firstName', firstName.trim());
    params.append('lastName', lastName.trim());

    const jwt = document.cookie.split('; ').find(row => row.startsWith('jwt='))?.split('=')[1];

    const response = await fetch(`http://localhost:8080/api/authors?${params.toString()}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${jwt}` }
    });

    if (!response.ok) throw new Error(await response.text());
    return response.json();
}

/**
 * Обновить автора
 */
export async function updateAuthor(id, firstName, lastName) {
    const params = new URLSearchParams();
    params.append('firstName', firstName.trim());
    params.append('lastName', lastName.trim());

    const jwt = document.cookie.split('; ').find(row => row.startsWith('jwt='))?.split('=')[1];

    const response = await fetch(`http://localhost:8080/api/authors/${id}?${params.toString()}`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${jwt}` }
    });

    if (!response.ok) throw new Error(await response.text());
    return response.json();
}

/**
 * Удалить автора
 */
export async function deleteAuthor(id) {
    return api.delete(`/authors/${id}`);
}