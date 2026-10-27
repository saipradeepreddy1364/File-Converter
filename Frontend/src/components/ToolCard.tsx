import React from 'react';
import type { ToolMetadata } from '../types';
import {
  Layers, Scissors, Trash2, RotateCw, Hash, Stamp, ArrowDownUp,
  ImageDown, FileText, Zap, Wrench, Minimize2, FileImage, FileType,
  Table, FileSpreadsheet, BookOpen, Image, Code, Globe,
  Grid, Lock, Unlock, EyeOff, GitCompare, ShieldCheck,
  Search, Sparkles, Repeat, Maximize, Sliders, Braces, QrCode, Scan,
  Fingerprint, FileCheck, FileEdit, Presentation
} from 'lucide-react';

interface ToolCardProps {
  tool: ToolMetadata;
  onClick: () => void;
}

const ICON_MAP: Record<string, React.ElementType> = {
  Layers, Scissors, Trash2, RotateCw, Hash, Stamp, ArrowDownUp,
  ImageDown, FileText, Zap, Wrench, Minimize2, FileImage, FileType,
  Table, FileSpreadsheet, BookOpen, Image, FileWord: FileEdit, Code, Globe,
  Grid, Lock, Unlock, EyeOff, GitCompare, ShieldCheck,
  ScanText: Scan, Search, Sparkles, Repeat, Maximize, Sliders, Braces,
  QrCode, Scan, Fingerprint, Presentation
};

export const ToolCard: React.FC<ToolCardProps> = ({ tool, onClick }) => {
  const IconComponent = ICON_MAP[tool.icon] || FileCheck;

  return (
    <div
      onClick={onClick}
      className="group relative cursor-pointer rounded-2xl glass-card glass-panel-hover p-5 border border-white/10 hover:border-indigo-500/40 transition-all flex flex-col justify-between"
    >
      <div>
        <div className="flex items-start justify-between mb-3.5">
          <div className="w-11 h-11 rounded-xl bg-slate-800/80 border border-white/10 flex items-center justify-center group-hover:scale-105 group-hover:bg-indigo-600/20 group-hover:border-indigo-500/30 transition-all">
            <IconComponent className="w-5 h-5 text-indigo-400 group-hover:text-indigo-300 transition-colors" />
          </div>
          {tool.badge && (
            <span
              className={`text-[10px] font-semibold tracking-wider uppercase px-2 py-0.5 rounded-full border ${
                tool.badge === 'In-House' || tool.badge === 'Core Engine'
                  ? 'bg-purple-500/20 text-purple-300 border-purple-500/30'
                  : tool.badge === 'Security'
                  ? 'bg-rose-500/20 text-rose-300 border-rose-500/30'
                  : tool.badge === 'AI Local'
                  ? 'bg-amber-500/20 text-amber-300 border-amber-500/30'
                  : 'bg-indigo-500/20 text-indigo-300 border-indigo-500/30'
              }`}
            >
              {tool.badge}
            </span>
          )}
        </div>

        <h3 className="text-base font-semibold text-white group-hover:text-indigo-300 transition-colors mb-1.5 flex items-center gap-1.5">
          {tool.name}
        </h3>
        <p className="text-xs text-slate-400 line-clamp-2 mb-4 leading-relaxed">
          {tool.description}
        </p>
      </div>

      <div className="pt-3 border-t border-white/5 flex items-center justify-between text-[11px] text-slate-500">
        <span className="font-mono">
          {tool.acceptedFormats.length > 0
            ? tool.acceptedFormats.slice(0, 3).join(', ')
            : 'Multi-Format'}
        </span>
        <span className="font-medium text-indigo-400 opacity-0 group-hover:opacity-100 transition-opacity">
          Open tool →
        </span>
      </div>
    </div>
  );
};
