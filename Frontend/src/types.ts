export interface ToolMetadata {
  id: string;
  name: string;
  category: string;
  description: string;
  icon: string;
  badge?: string;
  route: string;
  acceptedFormats: string[];
  outputFormats: string[];
  isMultiFile: boolean;
}

export interface BoundingBox {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface OcrWordResult {
  text: string;
  confidence: number;
  boundingBox: BoundingBox;
}

export interface OcrLineResult {
  text: string;
  confidence: number;
  boundingBox: BoundingBox;
  words: OcrWordResult[];
}

export interface OcrPageResult {
  pageNumber: number;
  width: number;
  height: number;
  text: string;
  confidence: number;
  lines: OcrLineResult[];
}

export interface OcrResult {
  jobId: string;
  originalFileName: string;
  fullText: string;
  averageConfidence: number;
  pageCount: number;
  processingTimeMs: number;
  language: string;
  engineInfo: string;
  lowQualityWarning: boolean;
  pages: OcrPageResult[];
  characterErrorRate?: number;
  wordErrorRate?: number;
}

export interface ConversionJob {
  id: string;
  toolName: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  message: string;
  progress: number;
  originalFileName?: string;
  outputFileName?: string;
  downloadUrl?: string;
  outputSizeBytes?: number;
  originalSizeBytes?: number;
  compressionRatio?: number;
  error?: string;
  metadata?: Record<string, any>;
}

export interface SampleResult {
  testCase: string;
  groundTruth: string;
  recognizedText: string;
  cer: number;
  wer: number;
  confidence: number;
  latencyMs: number;
}

export interface OcrEvaluationReport {
  datasetName: string;
  sampleCount: number;
  characterErrorRate: number;
  wordErrorRate: number;
  characterAccuracy: number;
  wordAccuracy: number;
  averageConfidence: number;
  averageLatencyMs: number;
  sampleResults: SampleResult[];
  limitations: string[];
}
