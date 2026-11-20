import React from 'react';
import { Download } from 'lucide-react';

export type WorkspaceSection = 'preview' | 'code' | 'architecture' | 'build';

interface TopBarProps {
  activeSection: WorkspaceSection;
  onNavigate: (section: WorkspaceSection) => void;
  onExportZip: () => void;
  isExporting: boolean;
}

export const TopBar: React.FC<TopBarProps> = ({
  activeSection,
  onNavigate,
  onExportZip,
  isExporting,
}) => {
  const links: Array<{ id: WorkspaceSection; label: string }> = [
    { id: 'preview', label: 'Aperçu Android' },
    { id: 'code', label: 'Projet Kotlin' },
    { id: 'architecture', label: 'Audit Matériel' },
    { id: 'build', label: 'Build APK' },
  ];

  return (
    <header className="sticky top-0 z-40 border-b border-[#23355E] bg-[#070D1F]/95 backdrop-blur-md">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-8 px-4 py-3 sm:px-6">
        <button
          type="button"
          onClick={() => onNavigate('preview')}
          className="text-left font-display text-lg font-bold tracking-tight text-white whitespace-nowrap shrink-0 focus-visible:outline-2 focus-visible:outline-[#3B82F6]"
        >
          BatteryShare AI
        </button>

        <nav aria-label="Navigation principale" className="hidden md:flex items-center gap-6">
          {links.map((link) => {
            const isActive = activeSection === link.id;
            return (
              <button
                key={link.id}
                type="button"
                onClick={() => onNavigate(link.id)}
                className={`min-h-[40px] text-sm font-medium transition-colors whitespace-nowrap shrink-0 border-b-2 ${
                  isActive
                    ? 'border-[#3B82F6] text-white'
                    : 'border-transparent text-[#94A3B8] hover:text-white'
                }`}
              >
                {link.label}
              </button>
            );
          })}
        </nav>

        <div className="flex items-center shrink-0">
          <button
            type="button"
            onClick={onExportZip}
            disabled={isExporting}
            className="inline-flex min-h-[40px] items-center gap-2 rounded-lg bg-[#2563EB] px-4 py-2 text-xs font-semibold text-white transition-colors hover:bg-[#1D4ED8] disabled:opacity-60 whitespace-nowrap shrink-0"
          >
            <Download className="h-4 w-4" aria-hidden="true" />
            <span>{isExporting ? 'Export...' : 'Exporter Projet (.zip)'}</span>
          </button>
        </div>
      </div>

      <nav
        aria-label="Navigation mobile"
        className="grid grid-cols-4 md:hidden gap-1 border-t border-[#162242] px-2 py-1.5"
      >
        {links.map((link) => {
          const isActive = activeSection === link.id;
          return (
            <button
              key={link.id}
              type="button"
              onClick={() => onNavigate(link.id)}
              className={`min-h-[38px] px-1 text-[11px] font-medium whitespace-nowrap truncate rounded-md transition-colors text-center ${
                isActive ? 'bg-[#152242] text-white' : 'text-[#94A3B8]'
              }`}
            >
              {link.label}
            </button>
          );
        })}
      </nav>
    </header>
  );
};
