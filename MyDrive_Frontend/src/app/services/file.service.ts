import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  private apiUrl = 'http://localhost:8080/api/files';

  constructor(private http: HttpClient) { }

  /**
   * Get all files for the current user
   */
  getAllFiles(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  /**
   * Get files in a specific directory
   * @param directoryId Directory ID
   */
  getFilesByDirectory(directoryId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/directory/${directoryId}`);
  }

  /**
   * Get file details by ID
   * @param id File ID
   */
  getFile(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  /**
   * Upload file to directory
   * @param file File to upload
   * @param directoryId Directory ID (optional)
   */
  uploadFile(file: File, directoryId?: number): Observable<HttpEvent<any>> {
    const formData: FormData = new FormData();
    formData.append('file', file);

    if (directoryId) {
      formData.append('directoryId', directoryId.toString());
    }

    const req = new HttpRequest('POST', `${this.apiUrl}/upload`, formData, {
      reportProgress: true,
      responseType: 'json'
    });

    return this.http.request(req);
  }

  /**
   * Upload folder (multiple files with relative paths)
   * @param files FileList or File[]
   * @param directoryId Directory ID (optional)
   */
  uploadFolder(files: FileList | File[], directoryId?: number) {
    const formData = new FormData();
    const fileArr = Array.from(files);
    fileArr.forEach(file => {
      formData.append('files', file);
      // @ts-ignore: webkitRelativePath is non-standard but supported by browsers
      formData.append('paths', (file as any).webkitRelativePath || file.name);
    });
    if (directoryId) {
      formData.append('directoryId', directoryId.toString());
    }

    // Thêm token JWT vào FormData để đảm bảo được gửi với request
    const token = localStorage.getItem('currentUser') ?
      JSON.parse(localStorage.getItem('currentUser')!).token : null;

    if (token) {
      formData.append('token', token);
    }

    return this.http.post(`${this.apiUrl}/upload-folder`, formData, {
      reportProgress: true,
      observe: 'events'
    });
  }

  /**
   * Download a file
   * @param id File ID
   */
  downloadFile(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/download`, {
      responseType: 'blob'
    });
  }

  /**
   * Delete a file
   * @param id File ID
   */
  deleteFile(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get file type icon
   * @param fileType MIME type
   */
  getFileIcon(fileType: string): string {
    if (!fileType) return 'description';

    if (fileType.startsWith('image/')) return 'image';
    if (fileType.startsWith('video/')) return 'movie';
    if (fileType.startsWith('audio/')) return 'audiotrack';
    if (fileType.includes('pdf')) return 'picture_as_pdf';
    if (fileType.includes('word') || fileType.includes('document')) return 'description';
    if (fileType.includes('excel') || fileType.includes('spreadsheet')) return 'table_chart';
    if (fileType.includes('presentation') || fileType.includes('powerpoint')) return 'slideshow';
    if (fileType.includes('text')) return 'article';
    if (fileType.includes('zip') || fileType.includes('compressed')) return 'folder_zip';

    return 'insert_drive_file';
  }

  /**
   * Format file size
   * @param bytes File size in bytes
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}
