export type UserRole = 'ADMIN' | 'USER' | 'CLIENT';
export type OrderStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED';

export interface User {
  id: string;
  login: string;
  role: UserRole;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
  user: User;
}

export interface Category {
  id: string;
  name: string;
}

export interface EventDto {
  id: string;
  title: string;
  description: string | null;
  location: string;
  /** Venue-local wall-clock time, e.g. "2030-06-01T20:00:00" (no timezone). */
  startsAt: string;
  capacity: number;
  ticketsSold: number;
  availableTickets: number;
  price: number;
  imageUrl: string | null;
  category: Category;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface OrderItem {
  eventId: string;
  eventTitle: string;
  eventLocation: string;
  eventStartsAt: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface Order {
  id: string;
  status: OrderStatus;
  totalAmount: number;
  /** Absolute instant (ISO-8601 with "Z"). */
  createdAt: string;
  /** Seconds a PENDING order still holds its tickets; null for other statuses. */
  expiresInSeconds: number | null;
  customerLogin: string;
  items: OrderItem[];
}

export interface TopEvent {
  id: string;
  title: string;
  ticketsSold: number;
  capacity: number;
  price: number;
}

export interface AdminStats {
  totalEvents: number;
  upcomingEvents: number;
  totalCategories: number;
  totalUsers: number;
  pendingOrders: number;
  completedOrders: number;
  cancelledOrders: number;
  ticketsSold: number;
  revenue: number;
  topEvents: TopEvent[];
}

export interface FieldViolation {
  field: string;
  message: string;
}

export interface EventFilters {
  search?: string;
  categoryId?: string;
  minPrice?: string;
  maxPrice?: string;
  from?: string;
  to?: string;
  onlyAvailable?: boolean;
  includePast?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export interface EventPayload {
  title: string;
  description: string;
  location: string;
  startsAt: string;
  capacity: number;
  price: number;
  imageUrl: string;
  categoryId: string;
}
