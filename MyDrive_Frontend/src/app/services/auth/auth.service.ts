import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { NotificationService } from '../notification.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api';
  private currentUserSubject: BehaviorSubject<any>;
  public currentUser: Observable<any>;

  constructor(
    private http: HttpClient,
    private notificationService: NotificationService
  ) {
    const storedUser = localStorage.getItem('currentUser');
    this.currentUserSubject = new BehaviorSubject<any>(storedUser ? JSON.parse(storedUser) : null);
    this.currentUser = this.currentUserSubject.asObservable();
  }

  public get currentUserValue(): any {
    return this.currentUserSubject.value;
  }
  login(username: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/auth/login`, { username, password })
      .pipe(
        tap(response => {
          if (response && response.token) {
            localStorage.setItem('currentUser', JSON.stringify(response));
            this.currentUserSubject.next(response);
            this.notificationService.success('Login successful!');
          }
          return response;
        }),
        catchError(this.handleError)
      );
  }

  register(username: string, email: string, password: string, fullName: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/auth/register`, {
      username,
      email,
      password,
      fullName
    }).pipe(
      tap(() => this.notificationService.success('Registration successful! Please login.')),
      catchError(this.handleError)
    );
  }

  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'An error occurred';

    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Error: ${error.error.message}`;
    } else {
      // Server-side error
      if (typeof error.error === 'string') {
        errorMessage = error.error;
      } else if (error.status === 401) {
        errorMessage = 'Invalid username or password';
      } else if (error.status === 400) {
        errorMessage = error.error || 'Bad request';
      } else if (error.status === 404) {
        errorMessage = 'Resource not found';
      } else {
        errorMessage = `Error Code: ${error.status}, Message: ${error.message}`;
      }
    }

    return throwError(() => errorMessage);
  }

  logout() {
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
  }

  isLoggedIn(): boolean {
    return this.currentUserValue !== null;
  }

  getToken(): string | null {
    return this.currentUserValue?.token || null;
  }
}
