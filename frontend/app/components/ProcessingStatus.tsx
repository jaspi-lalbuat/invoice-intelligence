"use client";

import {
  ProcessingStatus as ProcessingStatusType,
  formatStatus,
  getStatusColor,
} from "../lib/invoice";

type ProcessingStatusProps = {
  status: ProcessingStatusType | null;
  jobId: string | null;
  statusHistory: ProcessingStatusType[];
  refreshing: boolean;
  onRetry: () => void;
};

export default function ProcessingStatus({
  status,
  jobId,
  statusHistory,
  refreshing,
  onRetry,
}: ProcessingStatusProps) {
  return (
    <div className="border-b border-zinc-800 py-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-zinc-600">
            Processing status
          </p>

          <div className="mt-2 flex items-center gap-2">
            <span
              className={`h-2.5 w-2.5 rounded-full shadow-[0_0_8px_currentColor] ${
                status === "PROCESSING"
                  ? "animate-pulse"
                  : ""
              } ${
                status
                  ? getStatusColor(status)
                  : "bg-zinc-500"
              }`}
            />

            <span className="text-sm font-medium text-zinc-100">
              {status ? formatStatus(status) : "Loading..."}
            </span>
          </div>
        </div>

        {jobId && (
          <div className="text-right">
            <p className="text-xs text-zinc-600">
              Job ID
            </p>

            <p className="mt-1 max-w-[220px] truncate font-mono text-[11px] text-zinc-500">
              {jobId}
            </p>
          </div>
        )}
      </div>

      {status === "QUEUED" && (
        <p className="mt-2 text-xs text-zinc-600">
          Waiting for a processing worker...
        </p>
      )}

      {status === "PROCESSING" && (
        <p className="mt-2 text-xs text-zinc-600">
          Extracting and validating invoice...
        </p>
      )}

      {status === "READY" && (
        <p className="mt-2 text-xs text-zinc-600">
          Invoice extracted and all validation checks passed.
        </p>
      )}

      {status === "REVIEW_REQUIRED" && (
        <p className="mt-2 text-xs text-zinc-600">
          Extraction completed. Review the validation issues
          below.
        </p>
      )}

      {status === "FAILED" && (
        <>
          <p className="mt-2 text-xs text-zinc-600">
            Processing failed. The invoice can be retried.
          </p>

          <button
            type="button"
            onClick={onRetry}
            disabled={refreshing}
            className="mt-4 rounded-lg border border-zinc-700 px-3 py-2 text-sm text-zinc-200 transition hover:border-zinc-600 hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {refreshing ? "Retrying..." : "Retry processing"}
          </button>
        </>
      )}

      {statusHistory.length > 0 && (
        <div className="mt-5 flex flex-wrap items-center gap-2">
          {statusHistory.map((step, index) => (
            <div
              key={`${step}-${index}`}
              className="flex items-center gap-2"
            >
              <div
                className={`flex items-center gap-2 rounded-full border px-3 py-1.5 ${
                  index === statusHistory.length - 1
                    ? "border-zinc-600 bg-zinc-800"
                    : "border-zinc-800 bg-zinc-950/60"
                }`}
              >
                <span
                  className={`h-2 w-2 rounded-full ${getStatusColor(
                    step,
                  )}`}
                />

                <span className="text-xs font-medium text-zinc-400">
                  {formatStatus(step)}
                </span>
              </div>

              {index < statusHistory.length - 1 && (
                <span className="text-zinc-700">→</span>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}