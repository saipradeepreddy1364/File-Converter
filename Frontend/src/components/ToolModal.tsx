import React, { useState } from 'react';
import type { ToolMetadata, ConversionJob } from '../types';
import { apiService } from '../services/api';
import confetti from 'canvas-confetti';
import {
  X, UploadCloud, File as FileIcon, CheckCircle2,
  AlertTriangle, Loader2, Download, RefreshCw
} from 'lucide-react';

interface ToolModalProps {
  tool: ToolMetadata;
  onClose: () => void;
}

export const ToolModal: React.FC<ToolModalProps> = ({ tool, onClose }) => {
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ConversionJob | null>(null);

  // Form options
  const [password, setPassword] = useState('');
  const [watermarkText, setWatermarkText] = useState('CONFIDENTIAL');
  const [compressionLevel, setCompressionLevel] = useState<'low' | 'medium' | 'high'>('medium');
  const [targetFormat, setTargetFormat] = useState(tool.outputFormats[0]?.replace('.', '') || 'pdf');

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const newFiles = Array.from(e.dataTransfer.files);
      if (tool.isMultiFile) {
        setSelectedFiles((prev) => [...prev, ...newFiles]);
      } else {
        setSelectedFiles([newFiles[0]]);
      }
      setError(null);
    }
  };

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const newFiles = Array.from(e.target.files);
      if (tool.isMultiFile) {
        setSelectedFiles((prev) => [...prev, ...newFiles]);
      } else {
        setSelectedFiles([newFiles[0]]);
      }
      setError(null);
      e.target.value = '';
    }
  };

  const removeFile = (index: number) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const isMergeTool = tool.id === 'merge-pdf' || tool.name.toLowerCase().includes('merge');
  const isBelowMinimum = isMergeTool && selectedFiles.length < 2;

  const handleSubmit = async () => {
    if (selectedFiles.length === 0 && tool.acceptedFormats.length > 0) {
      setError('Please select a file to process.');
      return;
    }

    if (isMergeTool && selectedFiles.length < 2) {
      setError('Merging requires at least 2 PDF documents. Please add more files.');
      return;
    }

    setIsProcessing(true);
    setError(null);

    try {
      const formData = new FormData();
      if (tool.isMultiFile) {
        selectedFiles.forEach((file) => formData.append('files', file));
      } else if (selectedFiles.length > 0) {
        formData.append('file', selectedFiles[0]);
      }

      // Append tool-specific options
      if (tool.id.includes('protect') || tool.id.includes('unlock')) {
        formData.append('password', password);
      }
      if (tool.id.includes('watermark')) {
        formData.append('text', watermarkText);
      }
      if (tool.id.includes('compress')) {
        formData.append('level', compressionLevel);
      }
      if (tool.id.includes('format') || tool.id.includes('convert-image') || tool.id.startsWith('pdf-to-')) {
        let fmt = targetFormat;
        if (tool.id === 'pdf-to-jpg') fmt = 'jpg';
        else if (tool.id === 'pdf-to-docx') fmt = 'docx';
        else if (tool.id === 'pdf-to-txt') fmt = 'txt';
        else if (tool.id === 'pdf-to-markdown') fmt = 'md';
        else if (tool.id === 'pdf-to-html') fmt = 'html';
        else if (tool.id === 'pdf-to-tables') fmt = 'csv';
        formData.append('targetFormat', fmt || 'pdf');
      }

      const job = await apiService.executeTool(tool.route, formData);
      setResult(job);

      // Trigger celebration on success
      try {
        confetti({
          particleCount: 50,
          spread: 60,
          origin: { y: 0.7 }
        });
      } catch {
        // silent fail
      }
    } catch (err: any) {
      setError(err.message || 'Error processing request. Ensure the backend server is running and accessible.');
    } finally {
      setIsProcessing(false);
    }
  };

  const reset = () => {
    setSelectedFiles([]);
    setResult(null);
    setError(null);
  };

  const getActionLabel = (): string => {
    const name = tool.name;
    const lower = name.toLowerCase();

    if (lower.includes('to pdf')) {
      return 'Convert to PDF';
    }
    if (lower.includes('pdf to docx') || lower.includes('pdf to word')) {
      return 'Convert to Word (DOCX)';
    }
    if (lower.includes('pdf to images') || lower.includes('pdf to jpg')) {
      return 'Convert to Images';
    }
    if (lower.includes('pdf to text') || lower.includes('pdf to txt')) {
      return 'Extract to Text';
    }
    if (lower.includes('pdf to markdown')) {
      return 'Convert to Markdown';
    }
    if (lower.includes('pdf to html')) {
      return 'Convert to HTML';
    }
    if (lower.includes('pdf tables')) {
      return 'Extract Tables to CSV';
    }
    if (lower.includes('merge')) {
      return 'Merge PDFs';
    }
    if (lower.includes('split')) {
      return 'Split PDF';
    }
    if (lower.includes('compress')) {
      return 'Compress PDF';
    }
    if (lower.includes('protect') || lower.includes('encrypt')) {
      return 'Protect PDF';
    }
    if (lower.includes('unlock') || lower.includes('decrypt')) {
      return 'Unlock PDF';
    }
    if (lower.includes('watermark')) {
      return 'Add Watermark';
    }
    if (lower.includes('rotate')) {
      return 'Rotate PDF';
    }
    if (lower.includes('page number')) {
      return 'Add Page Numbers';
    }
    if (lower.includes('remove page')) {
      return 'Remove Pages';
    }
    if (lower.includes('reverse')) {
      return 'Reverse Pages';
    }
    if (lower.includes('redact')) {
      return 'Redact PDF';
    }
    if (lower.includes('compare')) {
      return 'Compare PDFs';
    }
    if (lower.includes('remove metadata') || lower.includes('sanitize')) {
      return 'Remove Metadata';
    }
    if (lower.includes('repair')) {
      return 'Repair PDF';
    }
    if (lower.includes('flatten')) {
      return 'Flatten PDF';
    }
    if (lower.includes('csv to xlsx') || lower.includes('to excel')) {
      return 'Convert to Excel';
    }
    if (lower.includes('xlsx to csv') || lower.includes('to csv')) {
      return 'Convert to CSV';
    }
    if (lower.includes('to json')) {
      return 'Convert to JSON';
    }
    if (lower.includes('format') && lower.includes('json')) {
      return 'Format JSON';
    }
    if (lower.includes('xml to json')) {
      return 'Convert XML / JSON';
    }
    if (lower.includes('qr') && lower.includes('generate')) {
      return 'Generate QR Code';
    }
    if (lower.includes('qr') && lower.includes('scan')) {
      return 'Scan QR Code';
    }
    if (lower.includes('hash')) {
      return 'Calculate Hashes';
    }
    if (lower.includes('word-count') || lower.includes('counter')) {
      return 'Count Words';
    }
    if (lower.includes('extract images')) {
      return 'Extract Images';
    }
    if (lower.includes('extract text')) {
      return 'Extract Text';
    }
    if (lower.includes('ocr')) {
      return 'Extract Text with OCR';
    }
    if (lower.includes('format converter') || lower.includes('convert-image')) {
      return `Convert Image to ${targetFormat.toUpperCase()}`;
    }
    if (lower.includes('resize')) {
      return 'Resize Image';
    }

    if (name.includes(' to ')) {
      const parts = name.split(' to ');
      return `Convert to ${parts[1]}`;
    }

    return name;
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
      <div className="relative w-full max-w-xl glass-panel rounded-3xl border border-white/10 shadow-2xl overflow-hidden bg-slate-900/95 flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="px-6 py-5 border-b border-white/10 flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                {tool.category}
              </span>
              <h2 className="text-lg font-bold text-white">{tool.name}</h2>
            </div>
            <p className="text-xs text-slate-400 mt-1">{tool.description}</p>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-slate-800 hover:bg-slate-700 flex items-center justify-center text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body */}
        <div className="p-6 overflow-y-auto space-y-5 flex-1">
          {error && (
            <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-start gap-3 text-rose-300 text-xs">
              <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold">Processing Notice</p>
                <p className="mt-0.5 text-rose-300/80">{error}</p>
              </div>
            </div>
          )}

          {!result ? (
            <>
              {/* Drag and Drop Zone */}
              <div
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                className={`border-2 border-dashed rounded-2xl p-8 text-center transition-all cursor-pointer flex flex-col items-center justify-center ${
                  isDragging
                    ? 'border-indigo-500 bg-indigo-500/10'
                    : 'border-white/15 hover:border-indigo-400/50 bg-slate-950/40'
                }`}
                onClick={() => document.getElementById('tool-file-input')?.click()}
              >
                <input
                  id="tool-file-input"
                  type="file"
                  multiple={tool.isMultiFile}
                  accept={tool.acceptedFormats.join(',')}
                  onChange={handleFileInput}
                  className="hidden"
                />
                <div className="w-14 h-14 rounded-2xl bg-indigo-600/10 border border-indigo-500/20 flex items-center justify-center mb-3">
                  <UploadCloud className="w-7 h-7 text-indigo-400" />
                </div>
                <p className="text-sm font-semibold text-slate-200">
                  {selectedFiles.length > 0
                    ? `${selectedFiles.length} file(s) selected`
                    : isMergeTool
                    ? 'Click or drag at least 2 PDF files to merge'
                    : 'Click or drag files here to upload'}
                </p>
                <p className="text-xs text-slate-400 mt-1">
                  {isMergeTool
                    ? 'Minimum 2 PDFs required • Formats: .pdf'
                    : `Supported formats: ${tool.acceptedFormats.join(', ') || 'Any file'}`}
                </p>
              </div>

              {/* Selected Files List */}
              {selectedFiles.length > 0 && (
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-300 uppercase tracking-wider">
                      Selected Files ({selectedFiles.length}{isMergeTool ? ' / 2 min' : ''})
                    </span>
                    {tool.isMultiFile && (
                      <button
                        type="button"
                        onClick={() => document.getElementById('tool-file-input')?.click()}
                        className="text-xs text-indigo-400 hover:text-indigo-300 font-medium cursor-pointer"
                      >
                        + Add more files
                      </button>
                    )}
                  </div>
                  <div className="max-h-36 overflow-y-auto space-y-1.5 pr-1">
                    {selectedFiles.map((file, idx) => (
                      <div
                        key={idx}
                        className="px-3 py-2 rounded-xl bg-slate-800/60 border border-white/5 flex items-center justify-between text-xs text-slate-300"
                      >
                        <div className="flex items-center gap-2 truncate">
                          <FileIcon className="w-4 h-4 text-indigo-400 shrink-0" />
                          <span className="truncate">{file.name}</span>
                        </div>
                        <div className="flex items-center gap-2 shrink-0">
                          <span className="text-slate-500 font-mono">
                            {(file.size / 1024).toFixed(1)} KB
                          </span>
                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              removeFile(idx);
                            }}
                            className="w-5 h-5 rounded hover:bg-slate-700 text-slate-400 hover:text-rose-400 flex items-center justify-center transition-colors cursor-pointer"
                            title="Remove file"
                          >
                            <X className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                  {isBelowMinimum && (
                    <p className="text-xs text-amber-400 font-medium">
                      ⚠️ Please add at least {2 - selectedFiles.length} more PDF to merge.
                    </p>
                  )}
                </div>
              )}

              {/* Tool Parameters */}
              {(tool.id.includes('protect') || tool.id.includes('unlock')) && (
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-300">
                    Document Password
                  </label>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Enter security password..."
                    className="w-full bg-slate-950/60 text-sm text-slate-200 px-3.5 py-2 rounded-xl border border-white/10 focus:outline-none focus:border-indigo-500"
                  />
                </div>
              )}

              {tool.id.includes('watermark') && (
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-300">
                    Watermark Text
                  </label>
                  <input
                    type="text"
                    value={watermarkText}
                    onChange={(e) => setWatermarkText(e.target.value)}
                    placeholder="e.g. CONFIDENTIAL, DRAFT"
                    className="w-full bg-slate-950/60 text-sm text-slate-200 px-3.5 py-2 rounded-xl border border-white/10 focus:outline-none focus:border-indigo-500"
                  />
                </div>
              )}

              {tool.id.includes('compress') && (
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-300">
                    Compression Level
                  </label>
                  <div className="grid grid-cols-3 gap-2">
                    {(['low', 'medium', 'high'] as const).map((lvl) => (
                      <button
                        key={lvl}
                        type="button"
                        onClick={() => setCompressionLevel(lvl)}
                        className={`py-2 rounded-xl text-xs font-medium uppercase tracking-wide border transition-all ${
                          compressionLevel === lvl
                            ? 'bg-indigo-600 text-white border-indigo-500 shadow-md shadow-indigo-600/30'
                            : 'bg-slate-800/60 text-slate-400 border-white/10 hover:bg-slate-800'
                        }`}
                      >
                        {lvl}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {(tool.id.includes('convert-image') || tool.id.includes('format')) && (
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-300">
                    Target Format
                  </label>
                  <div className="grid grid-cols-4 gap-2">
                    {['png', 'jpg', 'webp', 'bmp'].map((fmt) => (
                      <button
                        key={fmt}
                        type="button"
                        onClick={() => setTargetFormat(fmt)}
                        className={`py-2 rounded-xl text-xs font-medium uppercase tracking-wide border transition-all ${
                          targetFormat === fmt
                            ? 'bg-indigo-600 text-white border-indigo-500 shadow-md'
                            : 'bg-slate-800/60 text-slate-400 border-white/10 hover:bg-slate-800'
                        }`}
                      >
                        {fmt}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </>
          ) : (
            /* Result Section */
            <div className="p-6 rounded-2xl bg-indigo-950/20 border border-indigo-500/20 text-center space-y-4">
              <div className="w-14 h-14 rounded-full bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center mx-auto text-emerald-400">
                <CheckCircle2 className="w-7 h-7" />
              </div>

              <div>
                <h3 className="text-base font-bold text-white">Conversion Complete!</h3>
                <p className="text-xs text-slate-300 mt-1">{result.message}</p>
                {result.outputFileName && (
                  <p className="text-[11px] font-mono text-indigo-300 mt-1">
                    Output: {result.outputFileName}
                  </p>
                )}
                {result.compressionRatio && (
                  <p className="text-xs text-emerald-400 font-semibold mt-1">
                    Saved {(result.compressionRatio * 100).toFixed(1)}% file size!
                  </p>
                )}
              </div>

              <div className="flex items-center justify-center gap-3 pt-2">
                {result.outputFileName && (
                  <a
                    href={apiService.getDownloadUrl(result.outputFileName)}
                    download
                    className="flex items-center gap-2 px-5 py-2.5 rounded-xl font-semibold text-xs text-white bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 shadow-lg shadow-emerald-600/30 transition-all hover:scale-[1.02]"
                  >
                    <Download className="w-4 h-4" />
                    Download File
                  </a>
                )}
                <button
                  onClick={reset}
                  className="flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-medium text-slate-300 hover:text-white bg-slate-800/80 hover:bg-slate-800 border border-white/10 transition-colors"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  Convert Another
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-white/10 flex items-center justify-end gap-3 bg-slate-950/50">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-white transition-colors"
          >
            Close
          </button>
          {!result && (
            <button
              onClick={handleSubmit}
              disabled={isProcessing || isBelowMinimum}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-semibold text-white bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 shadow-lg shadow-indigo-600/25 disabled:opacity-40 disabled:cursor-not-allowed transition-all hover:scale-[1.02] active:scale-[0.98]"
            >
              {isProcessing ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Processing...</span>
                </>
              ) : isBelowMinimum ? (
                <span>{selectedFiles.length === 0 ? 'Select at least 2 PDFs' : 'Add at least 1 more PDF'}</span>
              ) : (
                <span>{getActionLabel()}</span>
              )}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
