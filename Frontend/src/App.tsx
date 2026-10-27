import { useState, useEffect, useMemo } from 'react';
import type { ToolMetadata } from './types';
import { DEFAULT_CATALOG } from './data/toolsCatalog';
import { apiService } from './services/api';
import { Navbar } from './components/Navbar';
import { ToolCard } from './components/ToolCard';
import { ToolModal } from './components/ToolModal';
import { OcrStudioModal } from './components/OcrStudioModal';
import { OcrBenchmarkModal } from './components/OcrBenchmarkModal';
import './App.css';
import {
  Sparkles, ShieldCheck, Zap, Cpu,
  Search, FileCheck2, ArrowRight
} from 'lucide-react';

function App() {
  const [catalog, setCatalog] = useState<Record<string, ToolMetadata[]>>(DEFAULT_CATALOG);
  const [selectedCategory, setSelectedCategory] = useState<string>('All');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedTool, setSelectedTool] = useState<ToolMetadata | null>(null);
  const [isOcrStudioOpen, setIsOcrStudioOpen] = useState(false);
  const [isBenchmarkOpen, setIsBenchmarkOpen] = useState(false);

  useEffect(() => {
    // Attempt to load dynamic catalog from backend if connected
    apiService.getCatalog()
      .then((data) => {
        if (data && Object.keys(data).length > 0) {
          setCatalog(data);
        }
      })
      .catch(() => {
        // Fallback to DEFAULT_CATALOG silently
      });
  }, []);

  // Flatten tools
  const allTools = useMemo(() => {
    const list: ToolMetadata[] = [];
    Object.values(catalog).forEach((categoryTools) => {
      list.push(...categoryTools);
    });
    return list;
  }, [catalog]);

  // Categories list
  const categories = useMemo(() => {
    return ['All', ...Object.keys(catalog)];
  }, [catalog]);

  // Filter tools based on search and category
  const filteredTools = useMemo(() => {
    return allTools.filter((tool) => {
      const matchesCategory =
        selectedCategory === 'All' || tool.category === selectedCategory;

      const q = searchQuery.toLowerCase().trim();
      const matchesSearch =
        !q ||
        tool.name.toLowerCase().includes(q) ||
        tool.description.toLowerCase().includes(q) ||
        tool.acceptedFormats.some((fmt) => fmt.toLowerCase().includes(q)) ||
        tool.outputFormats.some((fmt) => fmt.toLowerCase().includes(q));

      return matchesCategory && matchesSearch;
    });
  }, [allTools, selectedCategory, searchQuery]);

  return (
    <div className="min-h-screen bg-[#0b0f19] text-slate-100 flex flex-col selection:bg-indigo-500/30 selection:text-indigo-200">
      {/* Top Navbar */}
      <Navbar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        onOpenOcrStudio={() => setIsOcrStudioOpen(true)}
        onOpenBenchmark={() => setIsBenchmarkOpen(true)}
      />

      {/* Hero Section */}
      <section className="relative pt-12 pb-16 px-4 sm:px-6 lg:px-8 hero-radial-bg overflow-hidden border-b border-white/5">
        <div className="max-w-5xl mx-auto text-center space-y-6">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-xs font-medium shadow-sm">
            <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
            <span>Enterprise PDF Toolkit & In-House OCR Engine</span>
          </div>

          {/* Headline */}
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-white leading-[1.15]">
            Convert, Secure & Recognize <br />
            <span className="gradient-text">Any File or Document</span>
          </h1>

          {/* Subtitle */}
          <p className="max-w-2xl mx-auto text-sm sm:text-base text-slate-400 leading-relaxed">
            Privacy-first file conversion platform with 40+ utilities. Includes a proprietary
            neural-pattern OCR engine for scanned PDFs and images with zero cloud dependencies.
          </p>

          {/* Call to Actions */}
          <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
            <button
              onClick={() => setIsOcrStudioOpen(true)}
              className="flex items-center gap-2 px-5 py-3 rounded-2xl text-xs sm:text-sm font-semibold text-white bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 hover:from-indigo-500 hover:to-pink-500 shadow-xl shadow-indigo-600/25 border border-white/10 transition-all hover:scale-[1.02] active:scale-[0.98]"
            >
              <Sparkles className="w-4 h-4" />
              <span>Launch In-House OCR Studio</span>
              <ArrowRight className="w-4 h-4 ml-1" />
            </button>

            <button
              onClick={() => setIsBenchmarkOpen(true)}
              className="flex items-center gap-2 px-5 py-3 rounded-2xl text-xs sm:text-sm font-semibold text-slate-200 hover:text-white bg-slate-900/80 hover:bg-slate-800 border border-white/10 transition-all hover:scale-[1.02]"
            >
              <Cpu className="w-4 h-4 text-purple-400" />
              <span>Inspect Architecture & Benchmarks</span>
            </button>
          </div>

          {/* Quick Metrics */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-6 max-w-3xl mx-auto">
            <div className="p-3 rounded-2xl bg-slate-900/40 border border-white/5">
              <p className="text-xl font-bold text-white">40+</p>
              <p className="text-[11px] text-slate-400">Pro Tools Built-in</p>
            </div>
            <div className="p-3 rounded-2xl bg-slate-900/40 border border-white/5">
              <p className="text-xl font-bold text-emerald-400">100%</p>
              <p className="text-[11px] text-slate-400">Privacy & In-Memory</p>
            </div>
            <div className="p-3 rounded-2xl bg-slate-900/40 border border-white/5">
              <p className="text-xl font-bold text-purple-400">In-House</p>
              <p className="text-[11px] text-slate-400">Custom OCR Engine</p>
            </div>
            <div className="p-3 rounded-2xl bg-slate-900/40 border border-white/5">
              <p className="text-xl font-bold text-indigo-400">Java 21</p>
              <p className="text-[11px] text-slate-400">PDFBox 3 & Spring Boot</p>
            </div>
          </div>
        </div>
      </section>

      {/* Main Content Area */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 flex-1 w-full space-y-8">
        {/* Category Tabs */}
        <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
          {categories.map((cat) => {
            const count =
              cat === 'All'
                ? allTools.length
                : catalog[cat]?.length || 0;

            const isSelected = selectedCategory === cat;

            return (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className={`whitespace-nowrap px-4 py-2 rounded-xl text-xs font-semibold tracking-wide border transition-all flex items-center gap-2 ${
                  isSelected
                    ? 'bg-indigo-600 text-white border-indigo-500 shadow-lg shadow-indigo-600/20'
                    : 'bg-slate-900/60 text-slate-400 border-white/5 hover:bg-slate-800/80 hover:text-slate-200'
                }`}
              >
                <span>{cat}</span>
                <span
                  className={`text-[10px] px-1.5 py-0.2 rounded-md ${
                    isSelected ? 'bg-white/20 text-white' : 'bg-slate-800 text-slate-500'
                  }`}
                >
                  {count}
                </span>
              </button>
            );
          })}
        </div>

        {/* Section Header with result count */}
        <div className="flex items-center justify-between border-b border-white/5 pb-4">
          <div>
            <h2 className="text-lg font-bold text-white flex items-center gap-2">
              <span>{selectedCategory === 'All' ? 'Available Conversion & Processing Tools' : selectedCategory}</span>
              <span className="text-xs text-slate-500 font-normal">
                ({filteredTools.length} {filteredTools.length === 1 ? 'tool' : 'tools'})
              </span>
            </h2>
            {searchQuery && (
              <p className="text-xs text-indigo-400 mt-0.5">
                Filtered by &quot;{searchQuery}&quot;
              </p>
            )}
          </div>

          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="text-xs text-slate-400 hover:text-white underline underline-offset-4"
            >
              Clear search
            </button>
          )}
        </div>

        {/* Tools Grid */}
        {filteredTools.length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {filteredTools.map((tool) => (
              <ToolCard
                key={tool.id}
                tool={tool}
                onClick={() => setSelectedTool(tool)}
              />
            ))}
          </div>
        ) : (
          <div className="p-12 rounded-3xl glass-card text-center space-y-3 border border-white/10 max-w-md mx-auto">
            <Search className="w-8 h-8 text-slate-500 mx-auto" />
            <h3 className="text-base font-semibold text-white">No tools found</h3>
            <p className="text-xs text-slate-400">
              We couldn&apos;t find any tool matching &quot;{searchQuery}&quot;. Try searching for &quot;pdf&quot;, &quot;ocr&quot;, &quot;docx&quot;, or &quot;compress&quot;.
            </p>
            <button
              onClick={() => {
                setSearchQuery('');
                setSelectedCategory('All');
              }}
              className="mt-2 px-4 py-2 rounded-xl text-xs font-semibold text-indigo-400 bg-indigo-500/10 border border-indigo-500/20 hover:bg-indigo-500/20 transition-colors"
            >
              Reset Filters
            </button>
          </div>
        )}

        {/* Bottom Feature Showcase */}
        <section className="pt-10 border-t border-white/5">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            <div className="p-5 rounded-2xl glass-card border border-white/5 space-y-2">
              <div className="w-9 h-9 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 mb-3">
                <ShieldCheck className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-semibold text-white">100% Privacy by Design</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Documents are processed in isolated transient storage. Zero third-party telemetry, no AI model scraping, and automatic disk hygiene.
              </p>
            </div>

            <div className="p-5 rounded-2xl glass-card border border-white/5 space-y-2">
              <div className="w-9 h-9 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400 mb-3">
                <Sparkles className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-semibold text-white">Proprietary In-House OCR</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Includes connected component layout analysis, deskew projection algorithms, and multi-font prototype recognizers for searchable PDFs and DOCX.
              </p>
            </div>

            <div className="p-5 rounded-2xl glass-card border border-white/5 space-y-2">
              <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 mb-3">
                <Zap className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-semibold text-white">High-Throughput Engine</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Powered by Java 21 virtual threads, Apache PDFBox 3.0, Apache POI 5.3, and TwelveMonkeys image codecs for fast operations.
              </p>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="border-t border-white/10 bg-slate-950/80 py-8 px-4 sm:px-6 lg:px-8 mt-12 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <FileCheck2 className="w-4 h-4 text-indigo-400" />
            <span className="font-semibold text-slate-300">OmniConvert PRO</span>
            <span>— Enterprise Document Toolkit & In-House OCR Engine</span>
          </div>
          <div className="flex items-center gap-4 text-[11px] text-slate-400">
            <span>Spring Boot 3.3.4</span>
            <span>•</span>
            <span>React 19 + Vite</span>
            <span>•</span>
            <span>Tailwind CSS</span>
          </div>
        </div>
      </footer>

      {/* Modals */}
      {selectedTool && (
        <ToolModal
          tool={selectedTool}
          onClose={() => setSelectedTool(null)}
        />
      )}

      {isOcrStudioOpen && (
        <OcrStudioModal
          onClose={() => setIsOcrStudioOpen(false)}
        />
      )}

      {isBenchmarkOpen && (
        <OcrBenchmarkModal
          onClose={() => setIsBenchmarkOpen(false)}
        />
      )}
    </div>
  );
}

export default App;
