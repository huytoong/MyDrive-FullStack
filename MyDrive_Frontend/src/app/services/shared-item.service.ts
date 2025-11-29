import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SharedItemService {
  private apiUrl = 'http://localhost:8080/api/shared';

  constructor(private http: HttpClient) { }

  /**
   * Get items shared with the current user
   */
  getItemsSharedWithMe(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/with-me`);
  }

  /**
   * Get items shared by the current user
   */
  getItemsSharedByMe(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/by-me`);
  }

  /**
   * Share a file with another user
   * @param fileId File ID
   * @param username Username to share with
   * @param permissionLevel Permission level (view/edit)
   */
  shareFile(fileId: number, username: string, permissionLevel: string = 'view'): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/file/${fileId}`, {
      username,
      permissionLevel
    });
  }

  /**
   * Share a directory with another user
   * @param directoryId Directory ID
   * @param username Username to share with
   * @param permissionLevel Permission level (view/edit)
   */
  shareDirectory(directoryId: number, username: string, permissionLevel: string = 'view'): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/directory/${directoryId}`, {
      username,
      permissionLevel
    });
  }

  /**
   * Remove sharing for an item
   * @param sharedItemId Shared item ID
   */
  removeSharing(sharedItemId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${sharedItemId}`);
  }

  /**
   * Update sharing permissions
   * @param sharedItemId Shared item ID
   * @param permissionLevel New permission level
   */
  updateSharingPermissions(sharedItemId: number, permissionLevel: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${sharedItemId}`, {
      permissionLevel
    });
  }
}
