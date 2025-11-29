import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface Notification {
  message: string;
  type: 'success' | 'error' | 'info';
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationSubject = new BehaviorSubject<Notification | null>(null);
  notifications$: Observable<Notification | null> = this.notificationSubject.asObservable();
  private timeout: any;

  showNotification(notification: Notification) {
    // Clear any existing timeout
    if (this.timeout) {
      clearTimeout(this.timeout);
    }

    // Show new notification
    this.notificationSubject.next(notification);

    // Auto-hide after duration (default 5 seconds)
    const duration = notification.duration || 5000;
    this.timeout = setTimeout(() => {
      this.hideNotification();
    }, duration);
  }

  success(message: string, duration: number = 5000) {
    this.showNotification({
      message,
      type: 'success',
      duration
    });
  }

  error(message: string, duration: number = 5000) {
    this.showNotification({
      message,
      type: 'error',
      duration
    });
  }

  info(message: string, duration: number = 5000) {
    this.showNotification({
      message,
      type: 'info',
      duration
    });
  }

  hideNotification() {
    this.notificationSubject.next(null);
  }
}
