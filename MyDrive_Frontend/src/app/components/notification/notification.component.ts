import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService, Notification } from '../../services/notification.service';

@Component({
  selector: 'app-notification',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div *ngIf="notification" 
         class="notification-container"
         [ngClass]="{'success': notification.type === 'success',
                    'error': notification.type === 'error',
                    'info': notification.type === 'info'}">
      <div class="notification-content">
        <span class="notification-message">{{ notification.message }}</span>
        <button class="notification-close" (click)="close()">×</button>
      </div>
    </div>
  `,
  styles: [`
    .notification-container {
      position: fixed;
      top: 20px;
      right: 20px;
      padding: 1rem;
      border-radius: 4px;
      min-width: 300px;
      max-width: 500px;
      z-index: 9999;
      animation: slide-in 0.3s ease-out;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    }
    
    @keyframes slide-in {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }
    
    .notification-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    
    .notification-message {
      margin-right: 1rem;
      white-space: pre-wrap;
    }
    
    .notification-close {
      background: none;
      border: none;
      font-size: 1.5rem;
      cursor: pointer;
      color: inherit;
      opacity: 0.7;
    }
    
    .notification-close:hover {
      opacity: 1;
    }
    
    .success {
      background-color: #d4edda;
      color: #155724;
      border: 1px solid #c3e6cb;
    }
    
    .error {
      background-color: #f8d7da;
      color: #721c24;
      border: 1px solid #f5c6cb;
    }
    
    .info {
      background-color: #d1ecf1;
      color: #0c5460;
      border: 1px solid #bee5eb;
    }
  `]
})
export class NotificationComponent implements OnInit {
  notification: Notification | null = null;

  constructor(private notificationService: NotificationService) { }

  ngOnInit() {
    this.notificationService.notifications$.subscribe(notification => {
      this.notification = notification;
    });
  }

  close() {
    this.notificationService.hideNotification();
  }
}
