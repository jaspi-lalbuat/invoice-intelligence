export default function ProcessingPipeline() {
  return (
    <div className="mt-10 grid grid-cols-3 gap-3">
      {[
        ["01", "Extract", "PDF / OCR"],
        ["02", "Interpret", "LLM"],
        ["03", "Validate", "Deterministic"],
      ].map(([number, title, description]) => (
        <div
          key={number}
          className="rounded-xl border border-zinc-800/70 bg-zinc-900/30 p-4 transition hover:border-zinc-700"
        >
          <p className="text-xs text-zinc-600">
            {number}
          </p>

          <p className="mt-2 text-sm font-medium">
            {title}
          </p>

          <p className="mt-1 text-xs text-zinc-500">
            {description}
          </p>
        </div>
      ))}
    </div>
  );
}