import type { ToolMetadata, ConversionJob, OcrResult, OcrEvaluationReport } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export const apiService = {
  // Tool catalog
  async getCatalog(): Promise<Record<string, ToolMetadata[]>> {
    const res = await fetch(`${API_BASE_URL}/api/tools/catalog`);
    if (!res.ok) throw new Error('Failed to load tool catalog');
    return res.json();
  },

  // Health check
  async getHealth(): Promise<Record<string, any>> {
    const res = await fetch(`${API_BASE_URL}/api/health`);
    if (!res.ok) throw new Error('Failed to fetch backend health');
    return res.json();
  },

  // In-House OCR processing
  async processOcr(
    file: File,
    options: {
      language?: string;
      autoDeskew?: boolean;
      enhanceContrast?: boolean;
      outputFormat?: 'txt' | 'docx' | 'searchable_pdf';
    } = {}
  ): Promise<ConversionJob> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('language', options.language || 'eng');
    formData.append('autoDeskew', String(options.autoDeskew !== false));
    formData.append('enhanceContrast', String(options.enhanceContrast !== false));
    formData.append('outputFormat', options.outputFormat || 'txt');

    const res = await fetch(`${API_BASE_URL}/api/ocr/process`, {
      method: 'POST',
      body: formData,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ error: 'OCR processing failed' }));
      throw new Error(err.message || err.error || 'OCR processing failed');
    }
    return res.json();
  },

  // OCR result
  async getOcrResult(jobId: string): Promise<OcrResult> {
    const res = await fetch(`${API_BASE_URL}/api/ocr/result/${jobId}`);
    if (!res.ok) throw new Error('Failed to fetch OCR result');
    return res.json();
  },

  // OCR evaluation benchmark
  async runOcrEvaluation(): Promise<OcrEvaluationReport> {
    const res = await fetch(`${API_BASE_URL}/api/ocr/evaluate`, {
      method: 'POST',
    });
    if (!res.ok) throw new Error('Failed to run OCR evaluation');
    return res.json();
  },

  // OCR Engine architecture metadata
  async getOcrInfo(): Promise<Record<string, any>> {
    const res = await fetch(`${API_BASE_URL}/api/ocr/info`);
    if (!res.ok) throw new Error('Failed to load OCR architecture info');
    return res.json();
  },

  // Generic file conversion / tool execution
  async executeTool(endpoint: string, formData: FormData): Promise<ConversionJob> {
    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;
    const res = await fetch(url, {
      method: 'POST',
      body: formData,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ error: 'Operation failed' }));
      throw new Error(err.message || err.error || 'Operation failed on server');
    }
    return res.json();
  },

  // Direct download link helper
  getDownloadUrl(fileName: string): string {
    return `${API_BASE_URL}/api/download/${fileName}`;
  },
};
