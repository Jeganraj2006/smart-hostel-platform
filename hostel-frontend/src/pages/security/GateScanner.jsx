import { useEffect, useState, useRef } from 'react';
import { Html5QrcodeScanner } from 'html5-qrcode';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { gateService } from '../../services/gateService';

export default function GateScanner() {
    const [scanResult, setScanResult] = useState(null);
    const [manualLeaveId, setManualLeaveId] = useState('');
    const [processing, setProcessing] = useState(false);
    const scannerRef = useRef(null);

    const handleScan = async (text) => {
        if (processing) return;
        setProcessing(true);

        // Extract leaveId if formatted as leaveId:nonce
        let leaveId = text;
        if (text.includes(':')) {
            leaveId = text.split(':')[0];
        }

        try {
            const res = await gateService.scanGatepass(leaveId);
            setScanResult(res.data);
            toast.success(`Gatepass scanned successfully! Student is ${res.data.status}`);
        } catch (err) {
            const errMsg = err.response?.data?.error || err.response?.data || 'Failed to scan gatepass. Invalid or expired leave.';
            toast.error(errMsg);
        } finally {
            setProcessing(false);
        }
    };

    useEffect(() => {
        // Initialize HTML5 QR Code Scanner
        const scanner = new Html5QrcodeScanner(
            'reader',
            {
                fps: 10,
                qrbox: { width: 250, height: 250 },
                aspectRatio: 1.0,
            },
            false
        );

        scanner.render(
            (decodedText) => {
                handleScan(decodedText);
            },
            () => {
                // Verbose scanning logs omitted for performance
            }
        );

        scannerRef.current = scanner;

        return () => {
            if (scannerRef.current) {
                scannerRef.current.clear().catch((error) => {
                    console.error('Failed to clear html5QrcodeScanner: ', error);
                });
            }
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleManualSubmit = (e) => {
        e.preventDefault();
        if (!manualLeaveId.trim()) {
            toast.error('Please enter a valid Leave ID');
            return;
        }
        handleScan(manualLeaveId.trim());
    };

    return (
        <DashboardLayout>
            <div style={{ maxWidth: '800px', margin: '0 auto' }}>
                <div style={{ marginBottom: '24px' }}>
                    <h1 style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a' }}>📷 Gatepass QR Scanner</h1>
                    <p style={{ fontSize: '14px', color: '#64748b', marginTop: '4px' }}>
                        Scan student leave QR codes to record campus exit and entry logs.
                    </p>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
                    {/* Left: QR Reader & Manual Input */}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                        <div style={{ background: 'white', borderRadius: '12px', padding: '20px', border: '1px solid #e2e8f0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                            <h3 style={{ fontSize: '16px', fontWeight: '600', color: '#0f172a', marginBottom: '12px' }}>Camera Scanner</h3>
                            <div id="reader" style={{ overflow: 'hidden', borderRadius: '8px' }}></div>
                        </div>

                        <div style={{ background: 'white', borderRadius: '12px', padding: '20px', border: '1px solid #e2e8f0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                            <h3 style={{ fontSize: '16px', fontWeight: '600', color: '#0f172a', marginBottom: '12px' }}>Manual Entry Fallback</h3>
                            <form onSubmit={handleManualSubmit} style={{ display: 'flex', gap: '8px' }}>
                                <input
                                    type="text"
                                    placeholder="Enter Leave ID..."
                                    value={manualLeaveId}
                                    onChange={(e) => setManualLeaveId(e.target.value)}
                                    style={{
                                        flex: 1,
                                        border: '1px solid #e2e8f0',
                                        borderRadius: '8px',
                                        padding: '10px 14px',
                                        fontSize: '14px',
                                        outline: 'none',
                                    }}
                                />
                                <button
                                    type="submit"
                                    disabled={processing}
                                    style={{
                                        background: '#0D9488',
                                        color: 'white',
                                        border: 'none',
                                        borderRadius: '8px',
                                        padding: '10px 18px',
                                        fontSize: '14px',
                                        fontWeight: '500',
                                        cursor: 'pointer',
                                    }}
                                >
                                    {processing ? 'Processing...' : 'Scan'}
                                </button>
                            </form>
                        </div>
                    </div>

                    {/* Right: Scan Results */}
                    <div style={{ background: 'white', borderRadius: '12px', padding: '24px', border: '1px solid #e2e8f0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', display: 'flex', flexDirection: 'column', justifyContent: 'center', minHeight: '350px' }}>
                        {scanResult ? (
                            <div style={{ textAlign: 'center' }}>
                                <div style={{ fontSize: '48px', marginBottom: '16px' }}>
                                    {scanResult.status === 'OUT' ? '🛫' : '🛬'}
                                </div>
                                <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#0f172a' }}>Scan Verification</h2>
                                <span style={{
                                    display: 'inline-block',
                                    marginTop: '8px',
                                    padding: '4px 14px',
                                    borderRadius: '20px',
                                    fontSize: '12px',
                                    fontWeight: '600',
                                    background: scanResult.status === 'OUT' ? '#fef3c7' : '#d1fae5',
                                    color: scanResult.status === 'OUT' ? '#d97706' : '#059669'
                                }}>
                                    STUDENT IS {scanResult.status}
                                </span>

                                <div style={{ marginTop: '24px', textAlign: 'left', borderTop: '1px solid #f1f5f9', paddingTop: '20px' }}>
                                    <div style={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: '8px', fontSize: '14px' }}>
                                        <span style={{ color: '#64748b' }}>Leave ID:</span>
                                        <span style={{ color: '#0f172a', fontWeight: '500', wordBreak: 'break-all' }}>{scanResult.leaveId}</span>

                                        <span style={{ color: '#64748b' }}>Student ID:</span>
                                        <span style={{ color: '#0f172a', fontWeight: '500' }}>{scanResult.studentId}</span>

                                        <span style={{ color: '#64748b' }}>Exit Time:</span>
                                        <span style={{ color: '#0f172a', fontWeight: '500' }}>
                                            {scanResult.exitScannedAt ? new Date(scanResult.exitScannedAt).toLocaleString() : 'N/A'}
                                        </span>

                                        {scanResult.entryScannedAt && (
                                            <>
                                                <span style={{ color: '#64748b' }}>Entry Time:</span>
                                                <span style={{ color: '#0f172a', fontWeight: '500' }}>
                                                    {new Date(scanResult.entryScannedAt).toLocaleString()}
                                                </span>
                                            </>
                                        )}

                                        <span style={{ color: '#64748b' }}>Expected Return:</span>
                                        <span style={{ color: '#0f172a', fontWeight: '500' }}>
                                            {scanResult.expectedReturnAt ? new Date(scanResult.expectedReturnAt).toLocaleString() : 'N/A'}
                                        </span>
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <div style={{ textAlign: 'center', color: '#94a3b8' }}>
                                <div style={{ fontSize: '48px', marginBottom: '16px' }}>🎟️</div>
                                <h3 style={{ fontSize: '16px', fontWeight: '600', color: '#64748b' }}>Awaiting Scan</h3>
                                <p style={{ fontSize: '13px', color: '#94a3b8', marginTop: '4px', padding: '0 24px' }}>
                                    Scan a gatepass QR code or input the Leave ID manually to view verification details here.
                                </p>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </DashboardLayout>
    );
}
