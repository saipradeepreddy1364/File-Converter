import React, { useState } from 'react';
import type { OcrEvaluationReport } from '../types';
import { apiService } from '../services/api';
import {
  X, Cpu, Play, Loader2, CheckCircle, BarChart3,
  Layers, ShieldAlert, Zap
} from 'lucide-react';

interface OcrBenchmarkModalProps {
  onClose: () => void;
}

export const OcrBenchmarkModal: React.FC<OcrBenchmarkModalProps> = ({ onClose }) => {
  const [isRunning, setIsRunning] = useState(false);
  const [report, setReport] = useState<OcrEvaluationReport | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleRunEvaluation = async () => {
    setIsRunning(true);
    setError(null);
    try {
      const data = await apiService.runOcrEvaluation();
      setReport(data);
    } catch (err: any) {
      setError(err.message || 'Benchmark evaluation failed. Ensure backend service is reachable.');
    } finally {
      setIsRunning(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
      <div className="relative w-full max-w-3xl glass-panel rounded-3xl border border-white/10 shadow-2xl overflow-hidden bg-slate-900/95 flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="px-6 py-5 border-b border-white/10 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-600/20 border border-indigo-500/30 flex items-center justify-center text-indigo-400">
              <Cpu className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-bold text-white">OCR Engine Architecture & Benchmarks</h2>
                <span className="text-[10px] font-semibold tracking-wider uppercase px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                  Custom In-House
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Ground-truth validation suite measuring Character Error Rate (CER) and Word Error Rate (WER)
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-slate-800 hover:bg-slate-700 flex items-center justify-center text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1">
          {error && (
            <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-300 text-xs flex items-center gap-2">
              <ShieldAlert className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Architecture Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div className="p-4 rounded-2xl bg-slate-800/40 border border-white/5 space-y-1">
              <div className="flex items-center gap-2 text-indigo-400 text-xs font-semibold">
                <Layers className="w-4 h-4" />
                <span>Recognizer</span>
              </div>
              <p className="text-sm font-bold text-white">87 Prototypes</p>
              <p className="text-[11px] text-slate-400">
                Multi-font structural glyph matching across Serif, Sans-Serif & Mono.
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-slate-800/40 border border-white/5 space-y-1">
              <div className="flex items-center gap-2 text-purple-400 text-xs font-semibold">
                <Zap className="w-4 h-4" />
                <span>Preprocessing</span>
              </div>
              <p className="text-sm font-bold text-white">Radon / Projection</p>
              <p className="text-[11px] text-slate-400">
                Auto-deskew rotation, Otsu adaptive thresholding & noise filtering.
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-slate-800/40 border border-white/5 space-y-1">
              <div className="flex items-center gap-2 text-emerald-400 text-xs font-semibold">
                <BarChart3 className="w-4 h-4" />
                <span>Layout Analysis</span>
              </div>
              <p className="text-sm font-bold text-white">Connected Components</p>
              <p className="text-[11px] text-slate-400">
                Line segmentation, word clustering, and character-level bounding boxes.
              </p>
            </div>
          </div>

          {/* Benchmark Results */}
          {report ? (
            <div className="space-y-4">
              <div className="p-4 rounded-2xl bg-indigo-950/20 border border-indigo-500/20 space-y-4">
                <div className="flex items-center justify-between border-b border-white/10 pb-3">
                  <span className="text-xs font-semibold text-white uppercase tracking-wider">
                    Benchmark Results ({report.sampleCount} synthetic samples)
                  </span>
                  <span className="text-xs text-emerald-400 flex items-center gap-1 font-medium">
                    <CheckCircle className="w-3.5 h-3.5" /> Validation Complete
                  </span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center">
                  <div className="p-3 rounded-xl bg-slate-900/80 border border-white/5">
                    <p className="text-xs text-slate-400">Char Accuracy</p>
                    <p className="text-lg font-bold text-emerald-400 mt-0.5">
                      {(report.characterAccuracy * 100).toFixed(1)}%
                    </p>
                  </div>
                  <div className="p-3 rounded-xl bg-slate-900/80 border border-white/5">
                    <p className="text-xs text-slate-400">Word Accuracy</p>
                    <p className="text-lg font-bold text-emerald-400 mt-0.5">
                      {(report.wordAccuracy * 100).toFixed(1)}%
                    </p>
                  </div>
                  <div className="p-3 rounded-xl bg-slate-900/80 border border-white/5">
                    <p className="text-xs text-slate-400">Char Error Rate</p>
                    <p className="text-lg font-bold text-indigo-300 mt-0.5">
                      {(report.characterErrorRate * 100).toFixed(2)}%
                    </p>
                  </div>
                  <div className="p-3 rounded-xl bg-slate-900/80 border border-white/5">
                    <p className="text-xs text-slate-400">Avg Latency</p>
                    <p className="text-lg font-bold text-purple-300 mt-0.5">
                      {report.averageLatencyMs} ms
                    </p>
                  </div>
                </div>

                {report.limitations && report.limitations.length > 0 && (
                  <div className="pt-2">
                    <p className="text-xs font-semibold text-slate-300 mb-1">Documented Limitations:</p>
                    <ul className="text-[11px] text-slate-400 list-disc pl-4 space-y-0.5">
                      {report.limitations.map((lim, idx) => (
                        <li key={idx}>{lim}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="p-8 rounded-2xl border border-dashed border-white/10 text-center space-y-3 bg-slate-950/40">
              <BarChart3 className="w-10 h-10 text-slate-600 mx-auto" />
              <div>
                <p className="text-sm font-semibold text-slate-200">
                  Ready to run live benchmark suite
                </p>
                <p className="text-xs text-slate-400 mt-1 max-w-md mx-auto">
                  Calculates Levenshtein edit distance between ground truth synthetic test documents and our custom neural OCR recognizer.
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-white/10 flex items-center justify-between bg-slate-950/50">
          <span className="text-xs text-slate-500">
            Self-contained OCR engine without Tesseract or external cloud API
          </span>
          <div className="flex items-center gap-3">
            <button
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-white transition-colors"
            >
              Close
            </button>
            <button
              onClick={handleRunEvaluation}
              disabled={isRunning}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-semibold text-white bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 shadow-lg shadow-indigo-600/30 disabled:opacity-50 transition-all hover:scale-[1.02] active:scale-[0.98]"
            >
              {isRunning ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Running Benchmarks...</span>
                </>
              ) : (
                <>
                  <Play className="w-3.5 h-3.5 fill-current" />
                  <span>Run Evaluation Now</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
