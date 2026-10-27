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
import { Search, FileCheck2 } from 'lucide-react';

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

    // Ensure PowerPoint to PDF is on the first row (position 0)
    const pptxIdx = list.findIndex((t) => t.id === 'pptx-to-pdf');
    if (pptxIdx > 0) {
      const [pptxTool] = list.splice(pptxIdx, 1);
      list.unshift(pptxTool);
    }

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
      <section className="relative pt-10 pb-12 px-4 sm:px-6 lg:px-8 hero-radial-bg overflow-hidden border-b border-white/5">
        <div className="max-w-4xl mx-auto text-center space-y-4">
          {/* Headline */}
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-white leading-tight">
            Convert, Secure & Recognize <br />
            <span className="gradient-text">Any File or Document</span>
          </h1>

          {/* Subtitle */}
          <p className="max-w-2xl mx-auto text-sm sm:text-base text-slate-400 leading-relaxed">
            Privacy-first file conversion platform with 40+ utilities for PDF, Word, Excel, Images,
            OCR recognition, and document intelligence.
          </p>
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
