import React, { useState } from 'react';
import { apiService } from '../services/api';
import confetti from 'canvas-confetti';
import {
  X, Sparkles, UploadCloud, CheckCircle2,
  AlertCircle, Loader2, Download, Copy, Check, FileText
} from 'lucide-react';

interface OcrStudioModalProps {
  onClose: () => void;
}

export const OcrStudioModal: React.FC<OcrStudioModalProps> = ({ onClose }) => {
  const [file, setFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [autoDeskew, setAutoDeskew] = useState(true);
  const [enhanceContrast, setEnhanceContrast] = useState(true);
  const [outputFormat, setOutputFormat] = useState<'txt' | 'docx' | 'searchable_pdf'>('txt');

  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [resultText, setResultText] = useState<string | null>(null);
  const [downloadFileName, setDownloadFileName] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selected = e.target.files[0];
      setFile(selected);
      setError(null);
      setResultText(null);

      if (selected.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = () => setImagePreview(reader.result as string);
        reader.readAsDataURL(selected);
      } else {
        setImagePreview(null);
      }
    }
  };

  const handleProcess = async () => {
    if (!file) {
      setError('Please select an image or scanned PDF document to analyze.');
      return;
    }

    setIsProcessing(true);
    setError(null);
    setResultText(null);

    try {
      const job = await apiService.processOcr(file, {
        autoDeskew,
        enhanceContrast,
        outputFormat
      });

      if (job.outputFileName) {
        setDownloadFileName(job.outputFileName);
      }

      if (job.metadata && job.metadata.recognizedText) {
        setResultText(job.metadata.recognizedText);
      } else {
        setResultText(job.message || 'Text successfully recognized by In-House OCR engine.');
      }

      try {
        confetti({
          particleCount: 60,
          spread: 70,
          origin: { y: 0.6 }
        });
      } catch {
        // silent
      }
    } catch (err: any) {
      setError(err.message || 'OCR processing failed. Ensure backend service is reachable.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleCopy = () => {
    if (resultText) {
      navigator.clipboard.writeText(resultText);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
      <div className="relative w-full max-w-4xl glass-panel rounded-3xl border border-white/10 shadow-2xl overflow-hidden bg-slate-900/95 flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="px-6 py-5 border-b border-white/10 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-purple-600/20 border border-purple-500/30 flex items-center justify-center text-purple-400">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-bold text-white">In-House Neural OCR Studio</h2>
                <span className="text-[10px] font-semibold tracking-wider uppercase px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 border border-purple-500/30">
                  Zero External APIs
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Multi-font structural pattern recognizer with connected component analysis & skew correction
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
            <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-start gap-3 text-rose-300 text-xs">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold">Notice</p>
                <p className="mt-0.5 text-rose-300/80">{error}</p>
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Left: Input & Options */}
            <div className="space-y-4">
              <div
                onClick={() => document.getElementById('ocr-file-input')?.click()}
                className="border-2 border-dashed border-white/15 hover:border-purple-500/50 rounded-2xl p-6 text-center cursor-pointer bg-slate-950/40 flex flex-col items-center justify-center transition-all"
              >
                <input
                  id="ocr-file-input"
                  type="file"
                  accept=".png,.jpg,.jpeg,.webp,.pdf,.tiff,.bmp"
                  onChange={handleFileChange}
                  className="hidden"
                />
                {imagePreview ? (
                  <div className="space-y-2">
                    <img
                      src={imagePreview}
                      alt="Upload preview"
                      className="max-h-48 rounded-xl object-contain border border-white/10 mx-auto"
                    />
                    <p className="text-xs text-purple-300 font-medium">Click to replace file</p>
                  </div>
                ) : (
                  <>
                    <UploadCloud className="w-10 h-10 text-purple-400 mb-2" />
                    <p className="text-sm font-semibold text-slate-200">
                      {file ? file.name : 'Upload Document or Scanned Image'}
                    </p>
                    <p className="text-xs text-slate-400 mt-1">
                      PNG, JPG, WebP, TIFF, or scanned PDF
                    </p>
                  </>
                )}
              </div>

              {/* Engine Preprocessing Flags */}
              <div className="p-4 rounded-2xl bg-slate-800/40 border border-white/5 space-y-3">
                <span className="text-xs font-semibold text-slate-300 uppercase tracking-wider block">
                  Neural Preprocessing
                </span>
                <label className="flex items-center gap-2.5 text-xs text-slate-300 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={autoDeskew}
                    onChange={(e) => setAutoDeskew(e.target.checked)}
                    className="rounded border-white/20 text-purple-600 focus:ring-purple-500"
                  />
                  <span>Auto-Deskew & Orientation Normalization</span>
                </label>
                <label className="flex items-center gap-2.5 text-xs text-slate-300 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={enhanceContrast}
                    onChange={(e) => setEnhanceContrast(e.target.checked)}
                    className="rounded border-white/20 text-purple-600 focus:ring-purple-500"
                  />
                  <span>Adaptive Otsu Contrast Binarization</span>
                </label>
              </div>

              {/* Target Format */}
              <div className="space-y-2">
                <span className="text-xs font-semibold text-slate-300 uppercase tracking-wider block">
                  Export Format
                </span>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { id: 'txt', label: 'Plain Text' },
                    { id: 'docx', label: 'Word (.docx)' },
                    { id: 'searchable_pdf', label: 'Searchable PDF' }
                  ].map((fmt) => (
                    <button
                      key={fmt.id}
                      type="button"
                      onClick={() => setOutputFormat(fmt.id as any)}
                      className={`p-2 rounded-xl text-xs font-medium border transition-all ${
                        outputFormat === fmt.id
                          ? 'bg-purple-600 text-white border-purple-500 shadow-md shadow-purple-600/30'
                          : 'bg-slate-800/60 text-slate-400 border-white/10 hover:bg-slate-800'
                      }`}
                    >
                      {fmt.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* Right: Output & Text stream */}
            <div className="flex flex-col rounded-2xl bg-slate-950/60 border border-white/10 p-4 min-h-[300px]">
              <div className="flex items-center justify-between border-b border-white/10 pb-3 mb-3">
                <div className="flex items-center gap-2">
                  <FileText className="w-4 h-4 text-purple-400" />
                  <span className="text-xs font-semibold text-slate-300 uppercase tracking-wider">
                    Extracted Text
                  </span>
                </div>
                {resultText && (
                  <button
                    onClick={handleCopy}
                    className="flex items-center gap-1.5 text-xs text-slate-300 hover:text-white px-2.5 py-1 rounded-lg bg-slate-800/80 hover:bg-slate-800 border border-white/10 transition-colors"
                  >
                    {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    <span>{copied ? 'Copied' : 'Copy'}</span>
                  </button>
                )}
              </div>

              <div className="flex-1 overflow-y-auto">
                {resultText ? (
                  <pre className="text-xs text-slate-200 font-mono whitespace-pre-wrap leading-relaxed">
                    {resultText}
                  </pre>
                ) : (
                  <div className="h-full flex flex-col items-center justify-center text-slate-500 text-xs text-center p-6">
                    <Sparkles className="w-8 h-8 text-slate-600 mb-2" />
                    <p>Recognized text will appear here with character bounding boxes and confidence metrics.</p>
                  </div>
                )}
              </div>

              {downloadFileName && (
                <div className="pt-3 border-t border-white/10 flex items-center justify-between">
                  <span className="text-xs text-emerald-400 flex items-center gap-1.5 font-medium">
                    <CheckCircle2 className="w-4 h-4" /> Ready for download
                  </span>
                  <a
                    href={apiService.getDownloadUrl(downloadFileName)}
                    download
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-500 transition-colors shadow-md"
                  >
                    <Download className="w-3.5 h-3.5" />
                    Download File
                  </a>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-white/10 flex items-center justify-between bg-slate-950/50">
          <span className="text-xs text-slate-400">
            Supports Latin scripts, numerals, punctuation, and structural layout tables
          </span>
          <div className="flex items-center gap-3">
            <button
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-white transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handleProcess}
              disabled={isProcessing}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-semibold text-white bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 shadow-lg shadow-purple-600/30 disabled:opacity-50 transition-all hover:scale-[1.02] active:scale-[0.98]"
            >
              {isProcessing ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Recognizing Text...</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  <span>Start In-House OCR</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
