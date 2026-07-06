/** Mirrors the backend response envelopes (in.jlenterprises.ecommerce.response). */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface AuthUser {
  id: string;
  email: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  roles: string[]; // e.g. ["ROLE_SUPER_ADMIN"]
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}
