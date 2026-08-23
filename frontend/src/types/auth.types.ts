export type Role = 'ROLE_USER' | 'ROLE_ADMIN';

export interface User {
  id: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role: Role;
}

export interface UserSummaryResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummaryResponse;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}
