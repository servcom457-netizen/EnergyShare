import JSZip from 'jszip';
import { ANDROID_PROJECT_FILES } from '../generated/androidProjectBundle';

export async function downloadAndroidProjectZip(): Promise<{ fileCount: number; filename: string }> {
  const zip = new JSZip();
  const rootFolder = zip.folder('BatteryShareAI-v1.0');

  if (!rootFolder) {
    throw new Error('Impossible de créer l’archive ZIP.');
  }

  for (const file of ANDROID_PROJECT_FILES) {
    rootFolder.file(file.path, file.content);
  }

  const blob = await zip.generateAsync({ type: 'blob' });
  const filename = 'BatteryShareAI-v1.0-Android.zip';
  const url = URL.createObjectURL(blob);

  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  setTimeout(() => URL.revokeObjectURL(url), 1000);

  return { fileCount: ANDROID_PROJECT_FILES.length, filename };
}
