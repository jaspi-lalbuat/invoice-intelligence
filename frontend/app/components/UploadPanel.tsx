"use client";

import { useRef, useState } from "react";

type UploadPanelProps = {
  selectedFile: File | null;
  uploading: boolean;
  onFileSelected: (file: File | undefined) => void;
  onUpload: () => void;
};

export default function UploadPanel({
  selectedFile,
  uploading,
  onFileSelected,
  onUpload,
}: UploadPanelProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isDragging, setIsDragging] = useState(false);

  const handleFileChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    onFileSelected(event.target.files?.[0]);
  };

  return (
    <div className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-5">
      <div>
        <h3 className="text-sm font-medium text-white">
          Upload invoice
        </h3>

        <p className="mt-1 text-xs leading-5 text-zinc-500">
          PDF documents only
        </p>
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept="application/pdf"
        className="hidden"
        onChange={handleFileChange}
      />

      <div
        onDragOver={(event) => {
          event.preventDefault();
          setIsDragging(true);
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={(event) => {
          event.preventDefault();
          setIsDragging(false);
          onFileSelected(event.dataTransfer.files?.[0]);
        }}
        onClick={() => fileInputRef.current?.click()}
        className={`mt-5 w-full cursor-pointer rounded-xl border border-dashed px-4 py-7 text-center transition ${
          isDragging
            ? "border-zinc-400 bg-zinc-800/70"
            : "border-zinc-700 bg-zinc-950/60 hover:border-zinc-500 hover:bg-zinc-900"
        }`}
      >
        <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-lg border border-zinc-800 bg-zinc-900">
          <svg
            className="h-5 w-5 text-zinc-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={1.5}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M12 16V4m0 0 4 4m-4-4L8 8m9 4v5a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1v-5"
            />
          </svg>
        </div>

        <p className="mt-3 truncate text-sm font-medium text-zinc-300">
          {selectedFile
            ? selectedFile.name
            : "Drop your PDF here"}
        </p>

        <p className="mt-1 text-xs text-zinc-600">
          {selectedFile
            ? `${(selectedFile.size / 1024 / 1024).toFixed(2)} MB`
            : "or click to browse"}
        </p>
      </div>

      {selectedFile && (
        <button
          type="button"
          disabled={uploading}
          onClick={onUpload}
          className="mt-3 w-full rounded-xl bg-white px-4 py-2.5 text-sm font-medium text-zinc-950 transition hover:bg-zinc-200 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {uploading ? "Uploading..." : "Process invoice"}
        </button>
      )}
    </div>
  );
}