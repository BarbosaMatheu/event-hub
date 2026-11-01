import { request } from './client';
import type {
  AdminStats,
  AuthResponse,
  Category,
  EventDto,
  EventFilters,
  EventPayload,
  Order,
  OrderStatus,
  Page,
  User,
} from './types';

export const authApi = {
  login: (login: string, password: string) =>
    request<AuthResponse>('/auth/login', { method: 'POST', body: { login, password } }),
  register: (login: string, password: string) =>
    request<User>('/auth/register', { method: 'POST', body: { login, password } }),
  me: (signal?: AbortSignal) => request<User>('/auth/me', { signal }),
};

export const categoriesApi = {
  list: (signal?: AbortSignal) => request<Category[]>('/categories', { signal }),
  create: (name: string) => request<Category>('/categories', { method: 'POST', body: { name } }),
  update: (id: string, name: string) => request<Category>(`/categories/${id}`, { method: 'PUT', body: { name } }),
  remove: (id: string) => request<void>(`/categories/${id}`, { method: 'DELETE' }),
};

export const eventsApi = {
  search: (filters: EventFilters, signal?: AbortSignal) =>
    request<Page<EventDto>>('/events', { query: { ...filters }, signal }),
  get: (id: string, signal?: AbortSignal) => request<EventDto>(`/events/${id}`, { signal }),
  create: (payload: EventPayload) => request<EventDto>('/events', { method: 'POST', body: payload }),
  update: (id: string, payload: EventPayload) => request<EventDto>(`/events/${id}`, { method: 'PUT', body: payload }),
  remove: (id: string) => request<void>(`/events/${id}`, { method: 'DELETE' }),
};

export const ordersApi = {
  create: (eventId: string, quantity: number) =>
    request<Order>('/orders', { method: 'POST', body: { items: [{ eventId, quantity }] } }),
  mine: (page: number, signal?: AbortSignal) => request<Page<Order>>('/orders', { query: { page, size: 10 }, signal }),
  pay: (id: string) => request<Order>(`/orders/${id}/pay`, { method: 'POST' }),
  cancel: (id: string) => request<Order>(`/orders/${id}/cancel`, { method: 'POST' }),
};

export const adminApi = {
  stats: (signal?: AbortSignal) => request<AdminStats>('/admin/stats', { signal }),
  orders: (status: OrderStatus | '', page: number, signal?: AbortSignal) =>
    request<Page<Order>>('/admin/orders', { query: { status, page, size: 15 }, signal }),
  setOrderStatus: (id: string, status: OrderStatus) =>
    request<Order>(`/orders/${id}/status`, { method: 'PUT', body: { status } }),
};
