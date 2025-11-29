import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DirectoryService {
  private apiUrl = 'http://localhost:8080/api/directories';

  constructor(private http: HttpClient) { }

  /**
   * Get root directories for current user
   */
  getRootDirectories(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  /**
   * Get directory contents by ID
   * @param id Directory ID
   */
  getDirectoryContents(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  /**
   * Create a new directory
   * @param name Directory name
   * @param parentId Parent directory ID (optional)
   */
  createDirectory(name: string, parentId?: number): Observable<any> {
    const payload: any = { name };
    if (parentId !== undefined) {
      payload.parentId = parentId;
    }
    return this.http.post<any>(this.apiUrl, payload);
  }

  /**
   * Update directory name
   * @param id Directory ID
   * @param name New directory name
   */
  updateDirectory(id: number, name: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, { name });
  }

  /**
   * Delete a directory
   * @param id Directory ID
   */
  deleteDirectory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Download a directory as zip
   * @param id Directory ID
   */
  downloadDirectory(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/download`, {
      responseType: 'blob'
    });
  }

  /**
   * Build breadcrumb path
   * @param directories Directory list
   * @param currentId Current directory ID
   */
  buildBreadcrumb(directories: any[], currentId?: number): any[] {
    const result = [];

    if (!currentId) {
      return [{ id: null, name: 'My Drive', isRoot: true }];
    }

    // Add root
    result.push({ id: null, name: 'My Drive', isRoot: true });

    // Find the path
    const findPath = (dirs: any[], id: number, path: any[] = []): boolean => {
      for (const dir of dirs) {
        if (dir.id === id) {
          path.push({ id: dir.id, name: dir.name });
          return true;
        }

        if (dir.subdirectories && dir.subdirectories.length) {
          if (findPath(dir.subdirectories, id, path)) {
            path.unshift({ id: dir.id, name: dir.name });
            return true;
          }
        }
      }
      return false;
    };

    // Find the directory in the tree
    const allDirs = directories.flat();
    findPath(allDirs, currentId, result);

    return result;
  }
}
