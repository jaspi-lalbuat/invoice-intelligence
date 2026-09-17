"use client";

import {
  useEffect,
  useState,
} from "react";

import UploadPanel from "./components/UploadPanel";
import InvoiceList from "./components/InvoiceList";
import ProcessingStatus from "./components/ProcessingStatus";
import InvoiceResult from "./components/InvoiceResult";
import ProcessingPipeline from "./components/ProcessingPipeline";
import TechnicalDetails from "./components/TechnicalDetails";

import {
  InvoiceResponse,
  InvoiceSummary,
  ProcessingStatus as ProcessingStatusType,
} from "./lib/invoice";

import {
  getInvoice,
  getInvoices,
  uploadInvoice,
  retryInvoice,
  API_BASE_URL,
} from "./lib/api";

export default function Home() {
  const [selectedFile, setSelectedFile] =
    useState<File | null>(null);

  const [uploading, setUploading] = useState(false);

  const [invoices, setInvoices] = useState<
    InvoiceSummary[]
  >([]);

  const [selectedJobId, setSelectedJobId] =
    useState<string | null>(null);

  const [selectedInvoice, setSelectedInvoice] =
    useState<InvoiceResponse | null>(null);

  const [statusHistory, setStatusHistory] = useState<
    ProcessingStatusType[]
  >([]);

  const [error, setError] =
    useState<string | null>(null);

  const [refreshing, setRefreshing] = useState(false);

  const [pollingVersion, setPollingVersion] = useState(0);

  /*
   * Update the status of an invoice in the left-hand list.
   */
  const updateInvoiceInList = (
    jobId: string,
    status: ProcessingStatusType,
  ) => {
    setInvoices((previous) =>
      previous.map((invoice) =>
        invoice.jobId === jobId
          ? {
              ...invoice,
              status,
              updatedAt: new Date().toISOString(),
            }
          : invoice,
      ),
    );
  };

  /*
   * Load all previously uploaded invoices.
   */
    const loadInvoices = async () => {
      try {
        const data = await getInvoices();

        setInvoices(data);

        if (data.length > 0) {
          setSelectedJobId(
            (current) => current ?? data[0].jobId,
          );
        }
      } catch (err) {
        setError(
          err instanceof Error
            ? err.message
            : "Failed to load invoices.",
        );
      }
    };

  /*
   * Validate and select a PDF.
   */
  const selectFile = (file: File | undefined) => {
    setError(null);

    if (!file) {
      return;
    }

    if (file.type !== "application/pdf") {
      setSelectedFile(null);
      setError("Please select a PDF file.");
      return;
    }

    setSelectedFile(file);
  };

  /*
   * Load existing invoices when the page opens.
   */
  useEffect(() => {
    loadInvoices();
  }, []);

  /*
   * Fetch the complete details of the selected invoice.
   */
  useEffect(() => {
    if (!selectedJobId) {
      setSelectedInvoice(null);
      return;
    }

    let cancelled = false;

    const loadSelectedInvoice = async () => {
      try {
        const data = await getInvoice(selectedJobId);

        if (cancelled) {
          return;
        }

        setSelectedInvoice(data);
        setStatusHistory([data.status]);
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error
              ? err.message
              : "Failed to load invoice.",
          );
        }
      }
    };

    loadSelectedInvoice();

    return () => {
      cancelled = true;
    };
  }, [selectedJobId]);

  /*
   * Poll the selected invoice while it is processing.
   */
  useEffect(() => {
    if (!selectedJobId) {
      return;
    }

    let cancelled = false;
    let timeoutId: NodeJS.Timeout | null = null;

    const poll = async () => {
      try {
        const data = await getInvoice(selectedJobId);

        if (cancelled) {
          return;
        }

        setSelectedInvoice(data);

        setStatusHistory((previous) => {
          if (
            previous.length > 0 &&
            previous[previous.length - 1] ===
              data.status
          ) {
            return previous;
          }

          return [...previous, data.status];
        });

        updateInvoiceInList(
          data.jobId,
          data.status,
        );

        if (
          data.status === "READY" ||
          data.status === "REVIEW_REQUIRED" ||
          data.status === "FAILED"
        ) {
          return;
        }

        timeoutId = setTimeout(poll, 2000);
      } catch (err) {
        if (cancelled) {
          return;
        }

        console.error(
          "Status polling failed:",
          err,
        );

        timeoutId = setTimeout(poll, 2000);
      }
    };

    poll();

    return () => {
      cancelled = true;

      if (timeoutId) {
        clearTimeout(timeoutId);
      }
    };
  }, [selectedJobId, pollingVersion]);

  /*
   * Manually refresh the selected invoice.
   */
  const refreshStatus = async () => {
    if (!selectedJobId) {
      return;
    }

    setRefreshing(true);
    setError(null);

    try {
      const data = await getInvoice(selectedJobId);

      setSelectedInvoice(data);

      updateInvoiceInList(
        data.jobId,
        data.status,
      );

      setStatusHistory((previous) => {
        if (previous.includes(data.status)) {
          return previous;
        }

        return [...previous, data.status];
      });
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to refresh processing status.",
      );
    } finally {
      setRefreshing(false);
    }
  };

  /*
  * Manually retry job if failed
  */

  const handleRetry = async () => {
  if (!selectedJobId) {
    return;
  }

  setRefreshing(true);
  setError(null);

  try {
    await retryInvoice(selectedJobId);

    setSelectedInvoice((current) =>
      current
        ? {
            ...current,
            status: "QUEUED",
            invoice: null,
            validation: null,
          }
        : current,
    );

    updateInvoiceInList(selectedJobId, "QUEUED");

    setStatusHistory((previous) => {
      if (previous[previous.length - 1] === "QUEUED") {
        return previous;
      }

      return [...previous, "QUEUED"];
    });
    // Restart polling for the retried job.
    setPollingVersion((previous) => previous + 1);
  } catch (err) {
    setError(
      err instanceof Error
        ? err.message
        : "Failed to retry invoice processing.",
    );
  } finally {
    setRefreshing(false);
  }
};

  /*
   * Upload a new invoice.
   */
  const handleUpload = async () => {
    if (!selectedFile) {
      setError("Please select a PDF file first.");
      return;
    }

    setUploading(true);
    setError(null);

    try {
      const data = await uploadInvoice(selectedFile);
      const now = new Date().toISOString();

      const newInvoice: InvoiceSummary = {
        jobId: data.jobId,
        originalFileName: selectedFile.name,
        status: "QUEUED",
        createdAt: now,
        updatedAt: now,
      };

      setInvoices((previous) => [
        newInvoice,
        ...previous.filter(
          (invoice) =>
            invoice.jobId !== data.jobId,
        ),
      ]);

      setSelectedJobId(data.jobId);
      setSelectedFile(null);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Something went wrong while uploading.",
      );
    } finally {
      setUploading(false);
    }
  };

  const selectedSummary = invoices.find(
    (invoice) =>
      invoice.jobId === selectedJobId,
  );

  return (
    <main className="min-h-screen bg-zinc-950 text-zinc-100">
      <div className="mx-auto flex min-h-screen max-w-7xl flex-col px-6 py-8">
        {/* Header */}
        <header className="flex items-center justify-between">
          <div>
            <h1 className="text-xl font-semibold tracking-tight">
              Invoice Intelligence
            </h1>

            <p className="mt-1 text-sm text-zinc-400">
              AI-powered invoice extraction &
              validation
            </p>
          </div>

          <div className="rounded-full border border-zinc-800 bg-zinc-900 px-3 py-1.5 text-xs text-zinc-400">
            Local environment
          </div>
        </header>

        {/* Hero */}
        <section className="py-10">
          <div>
            <p className="mb-3 text-sm font-medium text-zinc-500">
              INVOICE PROCESSING
            </p>

            <h2 className="text-3xl font-semibold tracking-tight sm:text-[2.5rem]">
              Turn invoices into structured data.
            </h2>

            <p className="mt-2 max-w-xl text-sm leading-6 text-zinc-400">
              Upload a PDF and let the processing
              pipeline extract, interpret, and
              validate the invoice automatically.
            </p>
          </div>
        </section>

        {/* Main workspace */}
        <section className="grid flex-1 gap-6 lg:grid-cols-[300px_minmax(0,1fr)]">
          {/* Left column */}
          <aside className="space-y-5">
            <UploadPanel
              selectedFile={selectedFile}
              uploading={uploading}
              onFileSelected={selectFile}
              onUpload={handleUpload}
            />

            <InvoiceList
              invoices={invoices}
              selectedJobId={selectedJobId}
              onSelect={(jobId) => {
                setError(null);
                setSelectedJobId(jobId);
              }}
            />
          </aside>

          {/* Right column */}
          <div className="min-w-0 overflow-hidden rounded-2xl border border-zinc-800/80 bg-zinc-900/30">
            {!selectedJobId ? (
              <div className="flex min-h-[500px] items-center justify-center p-10 text-center">
                <div>
                  <p className="text-sm font-medium text-zinc-400">
                    Select an invoice
                  </p>

                  <p className="mt-2 text-xs text-zinc-600">
                    Upload an invoice or select one
                    from the list.
                  </p>
                </div>
              </div>
            ) : (
              <div className="p-6">
                {/* Selected invoice header */}
                <div className="flex flex-col gap-4 border-b border-zinc-800 pb-6 sm:flex-row sm:items-start sm:justify-between">
                  <div className="min-w-0">
                    <p className="text-[11px] font-medium uppercase tracking-[0.16em] text-zinc-600">
                      Invoice
                    </p>

                    <h3 className="mt-2 truncate text-2xl font-semibold tracking-tight text-white">
                      {selectedSummary?.originalFileName ??
                        "Invoice"}
                    </h3>

                    {selectedInvoice?.invoice
                      ?.invoiceNumber && (
                      <p className="mt-1 text-sm text-zinc-500">
                        {
                          selectedInvoice.invoice
                            .invoiceNumber
                        }
                      </p>
                    )}
                  </div>

                  <div className="flex gap-2">
                  <a
                    href={
                      selectedJobId
                        ? `${API_BASE_URL}/api/v1/documents/${selectedJobId}/file`
                        : undefined
                    }
                    download={selectedSummary?.originalFileName ?? undefined}
                    target="_blank"
                    rel="noreferrer"
                    className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-zinc-700 bg-zinc-900 text-zinc-400 transition hover:border-zinc-600 hover:bg-zinc-800 hover:text-zinc-200 disabled:cursor-not-allowed disabled:opacity-50"
                    aria-label="Download original file"
                  >
                    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M3 15v4a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1v-4M7 10l5 5 5-5M12 15V3" />
                    </svg>
                  </a>
                  </div>

                  <button
                    type="button"
                    onClick={refreshStatus}
                    disabled={refreshing}
                    title="Refresh status"
                    aria-label="Refresh status"
                    className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-zinc-700 bg-zinc-900 text-zinc-400 transition hover:border-zinc-600 hover:bg-zinc-800 hover:text-zinc-200 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <svg
                      className={`h-4 w-4 ${
                        refreshing ? "animate-spin" : ""
                      }`}
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      strokeWidth={1.5}
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        d="M20 11a8.1 8.1 0 0 0-15.5-2M4 5v4h4M4 13a8.1 8.1 0 0 0 15.5 2M20 19v-4h-4"
                      />
                    </svg>
                  </button>
                </div>

                {/* Status */}
                <ProcessingStatus
                  status={
                    selectedInvoice?.status ?? null
                  }
                  jobId={
                    selectedInvoice?.jobId ?? null
                  }
                  statusHistory={statusHistory}
                  refreshing={refreshing}
                  onRetry={handleRetry}
                />

                {/* Result */}
                {selectedInvoice ? (
                  <>
                    <TechnicalDetails invoice={selectedInvoice} />

                    <InvoiceResult invoice={selectedInvoice} />
                  </>
                ) : (
                  <div className="flex min-h-[350px] items-center justify-center text-center">
                    <div>
                      <div className="mx-auto h-5 w-5 animate-spin rounded-full border-2 border-zinc-700 border-t-zinc-300" />

                      <p className="mt-4 text-sm text-zinc-500">
                        Loading invoice...
                      </p>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </section>

        {/* Error */}
        {error && (
          <div className="mt-5 rounded-xl border border-red-900/50 bg-red-950/20 px-4 py-3 text-sm text-red-400">
            {error}
          </div>
        )}

        <ProcessingPipeline />

        {/* Footer */}
        <footer className="mt-8 flex items-center justify-between border-t border-zinc-900 pt-5 text-xs text-zinc-600">
          <span>Invoice Intelligence</span>

          <span>
            Spring Boot · PostgreSQL · Kafka · Ollama
          </span>
        </footer>
      </div>
    </main>
  );
}