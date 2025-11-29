import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpEventType, HttpResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth/auth.service';
import { DirectoryService } from '../../services/directory.service';
import { FileService } from '../../services/file.service';
import { SharedItemService } from '../../services/shared-item.service';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  currentUser: any;
  currentDirectory: number | null = null;
  directoryContents: any = { subdirectories: [], files: [] };
  breadcrumbs: any[] = [];
  isLoading = false;

  // Dialog states
  showUploadDialog = false;
  showShareDialog = false;
  showCreateFolderDialog = false;
  selectedItem: any = null;

  // View state
  viewMode: 'myFiles' | 'sharedWithMe' | 'sharedByMe' = 'myFiles';
  sharedWithMe: any[] = [];
  sharedByMe: any[] = [];

  // Form fields
  newFolderName = '';
  shareUsername = '';
  sharePermission = 'view';

  // File upload
  selectedFiles?: FileList;
  selectedFolderFiles?: FileList;
  currentFileUpload?: File;
  progress = 0;

  // Thêm biến để xác định đang xem thư mục được chia sẻ
  isViewingSharedDirectory: boolean = false;
  sharedDirectoryBreadcrumbs: any[] = [];
  currentSharedDirectoryId: number | null = null;

  constructor(
    private authService: AuthService,
    private directoryService: DirectoryService,
    private fileService: FileService,
    private sharedItemService: SharedItemService,
    private notificationService: NotificationService,
    private router: Router,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;

    // Subscribe to route params to get directory ID
    this.route.queryParams.subscribe(params => {
      if (params['dir']) {
        this.currentDirectory = Number(params['dir']);
        this.loadDirectoryContents(this.currentDirectory);
      } else {
        this.loadRootDirectories();
      }

      if (params['view']) {
        this.viewMode = params['view'];
        if (this.viewMode === 'sharedWithMe') {
          this.loadSharedWithMe();
        } else if (this.viewMode === 'sharedByMe') {
          this.loadSharedByMe();
        }
      }
    });
  }

  loadRootDirectories() {
    this.isLoading = true;
    this.currentDirectory = null;

    this.directoryService.getRootDirectories().subscribe({
      next: (directories) => {
        this.directoryContents = { subdirectories: directories, files: [] };
        this.breadcrumbs = [{ id: null, name: 'My Drive', isRoot: true }];
        this.isLoading = false;
        this.loadRootFiles();
      },
      error: (error) => {
        this.notificationService.error('Failed to load directories: ' + error);
        this.isLoading = false;
      }
    });
  }

  loadRootFiles() {
    this.isLoading = true;
    this.fileService.getAllFiles().subscribe({
      next: (files) => {
        // Only include files without a directory (root files)
        const rootFiles = files.filter(file => !file.directoryId);
        this.directoryContents.files = rootFiles;
        this.isLoading = false;
      },
      error: (error) => {
        this.notificationService.error('Failed to load files: ' + error);
        this.isLoading = false;
      }
    });
  }

  loadDirectoryContents(directoryId: number) {
    this.isLoading = true;
    this.directoryService.getDirectoryContents(directoryId).subscribe({
      next: (contents) => {
        this.directoryContents.subdirectories = contents.subdirectories || [];
        this.isLoading = false;
        // Now load files in this directory
        this.loadDirectoryFiles(directoryId);
        // Update breadcrumbs
        this.updateBreadcrumbs(directoryId, contents.name, contents.path);
      },
      error: (error) => {
        this.notificationService.error('Failed to load directory contents: ' + error);
        this.isLoading = false;
        // If directory not found, go back to root
        this.router.navigate(['/dashboard']);
      }
    });
  }

  loadDirectoryFiles(directoryId: number) {
    this.fileService.getFilesByDirectory(directoryId).subscribe({
      next: (files) => {
        this.directoryContents.files = files;
      },
      error: (error) => {
        this.notificationService.error('Failed to load files: ' + error);
      }
    });
  }

  updateBreadcrumbs(directoryId: number, directoryName: string, path: string) {
    // Parse the path string into breadcrumb components
    // Path format example: /Root/Folder1/Folder2
    this.breadcrumbs = [{ id: null, name: 'My Drive', isRoot: true }];

    if (path) {
      const parts = path.split('/').filter(p => p);
      let currentPath = '';

      parts.forEach((part, index) => {
        if (index < parts.length - 1) {
          // We don't have IDs for intermediate paths, will need to navigate via full path
          this.breadcrumbs.push({ name: part, path: (currentPath += '/' + part) });
        }
      });
    }

    // Add current directory as last breadcrumb if not root
    if (directoryId !== null) {
      this.breadcrumbs.push({ id: directoryId, name: directoryName });
    }
  }

  navigateToDirectory(directoryId: number | null) {
    if (directoryId === null) {
      this.router.navigate(['/dashboard'], { queryParams: { view: this.viewMode } });
    } else {
      this.router.navigate(['/dashboard'], { queryParams: { dir: directoryId, view: this.viewMode } });
    }
  }

  openCreateFolderDialog() {
    this.showCreateFolderDialog = true;
  }

  closeCreateFolderDialog() {
    this.showCreateFolderDialog = false;
    this.newFolderName = '';
  }

  createFolder() {
    if (!this.newFolderName.trim()) {
      this.notificationService.error('Folder name cannot be empty');
      return;
    }

    this.directoryService.createDirectory(this.newFolderName, this.currentDirectory !== null ? this.currentDirectory : undefined).subscribe({
      next: (newDirectory) => {
        this.notificationService.success(`Folder "${this.newFolderName}" created successfully`);
        this.closeCreateFolderDialog();

        // Refresh current directory content
        if (this.currentDirectory !== null) {
          this.loadDirectoryContents(this.currentDirectory);
        } else {
          this.loadRootDirectories();
        }
      },
      error: (error) => {
        this.notificationService.error('Failed to create folder: ' + error);
      }
    });
  }

  openUploadDialog() {
    this.showUploadDialog = true;
    this.progress = 0;
    this.selectedFiles = undefined;
    this.selectedFolderFiles = undefined;
  }

  closeUploadDialog() {
    this.showUploadDialog = false;
    this.selectedFiles = undefined;
    this.selectedFolderFiles = undefined;
    this.progress = 0;
  }

  selectFiles(event: any) {
    this.selectedFiles = event.target.files;
  }

  selectFolderFiles(event: any) {
    this.selectedFolderFiles = event.target.files;
  }

  uploadFiles() {
    if (!this.selectedFiles || this.selectedFiles.length === 0) {
      this.notificationService.error('Please select files to upload');
      return;
    }

    // Upload each file
    for (let i = 0; i < this.selectedFiles.length; i++) {
      this.upload(this.selectedFiles[i]);
    }

    this.showUploadDialog = false;
  }

  upload(file: File) {
    this.progress = 0;
    this.currentFileUpload = file;

    this.fileService.uploadFile(file, this.currentDirectory !== null ? this.currentDirectory : undefined).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress) {
          // Handle progress
          this.progress = Math.round(100 * event.loaded / (event.total || 1));
        } else if (event instanceof HttpResponse) {
          this.notificationService.success(`File "${file.name}" uploaded successfully`);

          // Refresh current directory content
          if (this.currentDirectory !== null) {
            this.loadDirectoryFiles(this.currentDirectory);
          } else {
            this.loadRootFiles();
          }
          this.currentFileUpload = undefined;
        }
      },
      error: (error) => {
        this.progress = 0;
        this.currentFileUpload = undefined;
        this.notificationService.error(`Failed to upload "${file.name}": ${error}`);
      }
    });
  }

  uploadFolderFiles() {
    if (!this.selectedFolderFiles || this.selectedFolderFiles.length === 0) {
      this.notificationService.error('Please select a folder to upload');
      return;
    }
    this.progress = 0;
    this.fileService.uploadFolder(this.selectedFolderFiles, this.currentDirectory !== null ? this.currentDirectory : undefined).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress) {
          this.progress = Math.round(100 * event.loaded / (event.total || 1));
        } else if (event instanceof HttpResponse) {
          this.notificationService.success('Folder uploaded successfully');
          if (this.currentDirectory !== null) {
            this.loadDirectoryFiles(this.currentDirectory);
            this.loadDirectoryContents(this.currentDirectory);
          } else {
            this.loadRootFiles();
            this.loadRootDirectories();
          }
          this.closeUploadDialog();
        }
      },
      error: (error) => {
        this.progress = 0;
        this.notificationService.error('Failed to upload folder: ' + error);
      }
    });
  }

  downloadFile(file: any) {
    this.fileService.downloadFile(file.id).subscribe({
      next: (data: Blob) => {
        // Create a download link and click it
        const downloadURL = window.URL.createObjectURL(data);
        const link = document.createElement('a');
        link.href = downloadURL;
        link.download = file.name;
        link.click();

        // Clean up
        window.URL.revokeObjectURL(downloadURL);
        this.notificationService.success(`File "${file.name}" downloaded successfully`);
      },
      error: (error) => {
        this.notificationService.error('Failed to download file: ' + error);
      }
    });
  }

  deleteItem(item: any, isDirectory: boolean) {
    const confirmDelete = confirm(`Are you sure you want to delete ${isDirectory ? 'folder' : 'file'} "${item.name}"?`);

    if (confirmDelete) {
      if (isDirectory) {
        this.directoryService.deleteDirectory(item.id).subscribe({
          next: () => {
            this.notificationService.success(`Folder "${item.name}" deleted successfully`);
            // Refresh current directory content
            if (this.currentDirectory !== null) {
              this.loadDirectoryContents(this.currentDirectory);
            } else {
              this.loadRootDirectories();
            }
          },
          error: (error) => {
            this.notificationService.error('Failed to delete folder: ' + error);
          }
        });
      } else {
        this.fileService.deleteFile(item.id).subscribe({
          next: () => {
            this.notificationService.success(`File "${item.name}" deleted successfully`);
            // Refresh current directory content
            if (this.currentDirectory !== null) {
              this.loadDirectoryFiles(this.currentDirectory);
            } else {
              this.loadRootFiles();
            }
          },
          error: (error) => {
            this.notificationService.error('Failed to delete file: ' + error);
          }
        });
      }
    }
  }

  openShareDialog(item: any, isDirectory: boolean) {
    this.selectedItem = { ...item, isDirectory };
    this.showShareDialog = true;
    this.shareUsername = '';
    this.sharePermission = 'view';
  }

  closeShareDialog() {
    this.showShareDialog = false;
    this.selectedItem = null;
  }

  shareItem() {
    if (!this.shareUsername.trim()) {
      this.notificationService.error('Username cannot be empty');
      return;
    }

    const { id, isDirectory } = this.selectedItem;

    if (isDirectory) {
      this.sharedItemService.shareDirectory(id, this.shareUsername, this.sharePermission).subscribe({
        next: (response) => {
          this.notificationService.success(`Folder shared with ${this.shareUsername} successfully`);
          this.closeShareDialog();
        },
        error: (error) => {
          this.notificationService.error('Failed to share folder: ' + error);
        }
      });
    } else {
      this.sharedItemService.shareFile(id, this.shareUsername, this.sharePermission).subscribe({
        next: (response) => {
          this.notificationService.success(`File shared with ${this.shareUsername} successfully`);
          this.closeShareDialog();
        },
        error: (error) => {
          this.notificationService.error('Failed to share file: ' + error);
        }
      });
    }
  }

  loadSharedWithMe() {
    this.isLoading = true;
    this.sharedItemService.getItemsSharedWithMe().subscribe({
      next: (items) => {
        // Đảm bảo itemDetails luôn có id và name cho file share đơn lẻ
        this.sharedWithMe = items.map(item => {
          if (item.itemType === 'file') {
            // Nếu thiếu itemDetails, tự tạo từ itemId và itemName
            if (!item.itemDetails) {
              item.itemDetails = { id: item.itemId, name: item.itemName };
            } else {
              // Đảm bảo có id và name
              item.itemDetails.id = item.itemId;
              item.itemDetails.name = item.itemName;
            }
          }
          return item;
        });
        this.isLoading = false;
      },
      error: (error) => {
        this.notificationService.error('Failed to load shared items: ' + error);
        this.isLoading = false;
      }
    });
  }

  loadSharedByMe() {
    this.isLoading = true;
    this.sharedItemService.getItemsSharedByMe().subscribe({
      next: (items) => {
        this.sharedByMe = items;
        this.isLoading = false;
      },
      error: (error) => {
        this.notificationService.error('Failed to load shared items: ' + error);
        this.isLoading = false;
      }
    });
  }

  switchView(view: 'myFiles' | 'sharedWithMe' | 'sharedByMe') {
    this.viewMode = view;

    if (view === 'myFiles') {
      this.router.navigate(['/dashboard'], { queryParams: { view } });
    } else if (view === 'sharedWithMe') {
      this.router.navigate(['/dashboard'], { queryParams: { view } });
      this.loadSharedWithMe();
    } else if (view === 'sharedByMe') {
      this.router.navigate(['/dashboard'], { queryParams: { view } });
      this.loadSharedByMe();
    }
  }

  removeSharing(sharedItem: any) {
    const confirmRemove = confirm('Are you sure you want to remove sharing for this item?');

    if (confirmRemove) {
      this.sharedItemService.removeSharing(sharedItem.id).subscribe({
        next: () => {
          this.notificationService.success('Sharing removed successfully');
          // Refresh shared items
          if (this.viewMode === 'sharedWithMe') {
            this.loadSharedWithMe();
          } else if (this.viewMode === 'sharedByMe') {
            this.loadSharedByMe();
          }
        },
        error: (error) => {
          this.notificationService.error('Failed to remove sharing: ' + error);
        }
      });
    }
  }

  getFileTypeIcon(fileType: string): string {
    return this.fileService.getFileIcon(fileType);
  }

  formatFileSize(bytes: number): string {
    return this.fileService.formatFileSize(bytes);
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  // Hàm xem chi tiết file cho nút Xem chi tiết
  viewFileDetails(file: any): void {
    this.fileService.getFile(file.id).subscribe(
      (details: any) => {
        alert(
          `Tên: ${details.name}\nLoại: ${details.type}\nKích thước: ${this.formatFileSize(details.size)}\nTải lên: ${details.createdAt}\nCập nhật: ${details.updatedAt}`
        );
      },
      (error: any) => {
        this.notificationService.error('Không lấy được thông tin file!');
      }
    );
  }

  /**
   * Mở thư mục được chia sẻ (ở chế độ "Shared with me")
   */
  openSharedDirectory(item: any) {
    if (item.itemType !== 'directory') return;
    this.isLoading = true;
    this.isViewingSharedDirectory = true;
    this.currentSharedDirectoryId = item.itemId;
    this.directoryService.getDirectoryContents(item.itemId).subscribe({
      next: (contents) => {
        this.directoryContents.subdirectories = contents.subdirectories || [];
        this.directoryContents.files = contents.files || [];
        // Breadcrumbs cho thư mục được chia sẻ
        this.sharedDirectoryBreadcrumbs = this.buildSharedDirectoryBreadcrumbs(contents.path, contents.name, item.itemId);
        this.isLoading = false;
      },
      error: (error) => {
        this.notificationService.error('Không thể truy cập thư mục được chia sẻ!');
        this.isLoading = false;
      }
    });
  }

  /**
   * Xây dựng breadcrumbs cho thư mục được chia sẻ
   */
  buildSharedDirectoryBreadcrumbs(path: string, directoryName: string, directoryId: number) {
    const breadcrumbs = [{ id: null, name: 'Shared with me', isRoot: true }];
    if (path) {
      const parts = path.split('/').filter(p => p);
      for (let i = 0; i < parts.length - 1; i++) {
        breadcrumbs.push({ id: null, name: parts[i], isRoot: false });
      }
    }
    breadcrumbs.push({ id: null, name: directoryName, isRoot: false });
    return breadcrumbs;
  }

  /**
   * Quay lại danh sách "Shared with me"
   */
  backToSharedWithMe() {
    this.isViewingSharedDirectory = false;
    this.currentSharedDirectoryId = null;
    this.sharedDirectoryBreadcrumbs = [];
    this.directoryContents = { subdirectories: [], files: [] };
    this.loadSharedWithMe();
  }
}
