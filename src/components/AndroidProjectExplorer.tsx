import React, { useMemo, useState } from 'react';
import { Check, CheckCircle2, Copy, FileCode2, FolderTree, Search } from 'lucide-react';
import {
  ANDROID_PROJECT_FILES,
  ANDROID_VERIFICATION_CHECKS,
  type AndroidProjectFile,
} from '../generated/androidProjectBundle';

export const AndroidProjectExplorer: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [selectedFile, setSelectedFile] = useState<AndroidProjectFile>(
    () =>
      ANDROID_PROJECT_FILES.find((f) => f.path.endsWith('MainActivity.kt')) ??
      ANDROID_PROJECT_FILES[0]
  );
  const [copiedPath, setCopiedPath] = useState<string | null>(null);

  const groupForCategory = (cat: string): string => {
    if (cat === 'UI Jetpack Compose') return 'UI Compose';
    if (cat === 'Contrôleurs Matériels & Services' || cat === 'Domaine & Protections') return 'Matériel & Domaine';
    if (cat === 'Base Room & Repository') return 'Room & Data';
    if (cat === 'Gradle & Configuration' || cat === 'Manifest' || cat === 'Ressources XML & Graphiques' || cat === 'Application Android') return 'Gradle & Ressources';
    return 'Tests, CI & APK';
  };

  const categories = ['ALL', 'UI Compose', 'Matériel & Domaine', 'Room & Data', 'Gradle & Ressources', 'Tests, CI & APK'];

  const filteredFiles = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return ANDROID_PROJECT_FILES.filter((file) => {
      const matchesCategory = selectedCategory === 'ALL' || groupForCategory(file.category) === selectedCategory;
      const matchesQuery =
        q === '' ||
        file.path.toLowerCase().includes(q) ||
        file.category.toLowerCase().includes(q);
      return matchesCategory && matchesQuery;
    });
  }, [searchQuery, selectedCategory]);

  const totalLines = useMemo(
    () => ANDROID_PROJECT_FILES.reduce((acc, f) => acc + f.lines, 0),
    []
  );

  const passedChecks = ANDROID_VERIFICATION_CHECKS.filter((c) => c.passed).length;

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(selectedFile.content);
      setCopiedPath(selectedFile.path);
      setTimeout(() => setCopiedPath(null), 1500);
    } catch {
      // Clipboard blocked in non-HTTPS contexts
    }
  };

  return (
    <section aria-labelledby="explorer-heading" className="mx-auto max-w-7xl px-4 py-8 sm:px-6 space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4 border-b border-[#23355E] pb-5">
        <div>
          <p className="text-xs font-medium text-[#3B82F6]">
            Livrable complet · {ANDROID_PROJECT_FILES.length} fichiers · {totalLines.toLocaleString('fr-FR')} lignes
          </p>
          <h2 id="explorer-heading" className="mt-1 font-display text-2xl sm:text-3xl font-bold text-white">
            Explorateur du projet Android Studio (Kotlin &amp; Gradle)
          </h2>
        </div>

        <div className="flex items-center gap-2 text-xs font-mono text-[#10B981]">
          <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
          <span>
            Vérification structurelle : {passedChecks}/{ANDROID_VERIFICATION_CHECKS.length} contrôles validés
          </span>
        </div>
      </div>

      {/* Category Filter Bar */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1">
        {categories.map((cat) => {
          const active = selectedCategory === cat;
          return (
            <button
              key={cat}
              type="button"
              onClick={() => setSelectedCategory(cat)}
              className={`min-h-[38px] rounded-lg px-3 py-1.5 text-xs font-medium transition-colors whitespace-nowrap shrink-0 ${
                active
                  ? 'bg-[#2563EB] text-white'
                  : 'border border-[#23355E] bg-[#0E1730] text-[#94A3B8] hover:text-white'
              }`}
            >
              {cat === 'ALL' ? `Tous (${ANDROID_PROJECT_FILES.length})` : cat}
            </button>
          );
        })}
      </div>

      {/* Split IDE Explorer */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        {/* File Tree */}
        <div className="lg:col-span-4 rounded-2xl border border-[#23355E] bg-[#0E1730] overflow-hidden flex flex-col max-h-[680px]">
          <div className="border-b border-[#162242] p-3">
            <label htmlFor="android-file-search" className="sr-only">
              Filtrer les fichiers Android
            </label>
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-[#94A3B8]" />
              <input
                id="android-file-search"
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Rechercher un fichier Kotlin, XML, Gradle..."
                className="w-full rounded-lg border border-[#23355E] bg-[#070D1F] py-2 pl-9 pr-3 text-xs text-white placeholder-[#64748B] focus:border-[#3B82F6] focus:outline-none"
              />
            </div>
          </div>

          <div className="overflow-y-auto divide-y divide-[#162242]">
            {filteredFiles.map((file) => {
              const active = selectedFile.path === file.path;
              return (
                <button
                  key={file.path}
                  type="button"
                  onClick={() => setSelectedFile(file)}
                  className={`flex w-full items-start gap-2.5 px-3.5 py-2.5 text-left transition-colors ${
                    active ? 'bg-[#152242]' : 'hover:bg-[#111C38]'
                  }`}
                >
                  <FileCode2
                    className={`mt-0.5 h-4 w-4 shrink-0 ${
                      active ? 'text-[#3B82F6]' : 'text-[#94A3B8]'
                    }`}
                    aria-hidden="true"
                  />
                  <div className="min-w-0 flex-1">
                    <p
                      className={`font-mono text-xs truncate ${
                        active ? 'font-semibold text-white' : 'text-[#E2E8F0]'
                      }`}
                    >
                      {file.path.split('/').pop()}
                    </p>
                    <p className="font-mono text-[10px] text-[#94A3B8] truncate">
                      {file.path} · {file.lines} l.
                    </p>
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        {/* Code Viewer */}
        <div className="lg:col-span-8 rounded-2xl border border-[#23355E] bg-[#0B1226] overflow-hidden flex flex-col max-h-[680px]">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#23355E] bg-[#0E1730] px-4 py-3">
            <div className="flex items-center gap-2 min-w-0">
              <FolderTree className="h-4 w-4 text-[#3B82F6] shrink-0" aria-hidden="true" />
              <span className="font-mono text-xs text-white truncate">
                {selectedFile.path}
              </span>
              <span className="text-xs text-[#94A3B8] shrink-0">
                · {selectedFile.lines} lignes
              </span>
            </div>

            <button
              type="button"
              onClick={handleCopy}
              className="inline-flex min-h-[34px] items-center gap-1.5 rounded-lg border border-[#23355E] bg-[#152242] px-3 py-1.5 text-xs font-medium text-white hover:bg-[#1E2F5A] whitespace-nowrap shrink-0"
            >
              {copiedPath === selectedFile.path ? (
                <>
                  <Check className="h-3.5 w-3.5 text-[#10B981]" />
                  <span>Copié</span>
                </>
              ) : (
                <>
                  <Copy className="h-3.5 w-3.5" />
                  <span>Copier le fichier</span>
                </>
              )}
            </button>
          </div>

          <pre className="flex-1 overflow-auto p-4 font-mono text-xs leading-relaxed text-[#E2E8F0]">
            <code>{selectedFile.content}</code>
          </pre>
        </div>
      </div>

      {/* Structural Verification Matrix */}
      <div className="rounded-2xl border border-[#23355E] bg-[#0E1730] p-5">
        <h3 className="font-display text-base font-semibold text-white mb-3">
          Contrôles de cohérence statique du livrable Android
        </h3>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
          {ANDROID_VERIFICATION_CHECKS.map((check) => (
            <div
              key={check.id}
              className="flex items-start gap-2 rounded-lg border border-[#162242] bg-[#070D1F] px-3 py-2 text-xs"
            >
              <CheckCircle2
                className={`mt-0.5 h-3.5 w-3.5 shrink-0 ${
                  check.passed ? 'text-[#10B981]' : 'text-[#EF4444]'
                }`}
                aria-hidden="true"
              />
              <span className="text-[#E2E8F0]">{check.label}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
