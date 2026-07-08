import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';
import DashboardLayout from '../../components/layout/DashboardLayout';

const translations = {
    en: {
        title: "Student Trust & Activity Timeline",
        subtitle: "Real-time consolidated tracking of leaves, fees, complaints, and gate scans for your ward.",
        langToggle: "தமிழ் (Tamil)",
        loading: "Loading timeline activities...",
        noEvents: "No activities recorded on the timeline yet.",
        errorTitle: "Error Loading Timeline",
        errorDesc: "Could not retrieve the student timeline. Please try again later.",
        timestamp: "Date & Time",
        type: "Type",
        status: "Status",
        types: {
            LEAVE: "Leave request",
            FEE: "Fee payment status",
            COMPLAINT: "Complaint raised",
            GATE_EXIT: "Hostel gate checkout",
            GATE_ENTRY: "Hostel gate check-in"
        },
        statuses: {
            PENDING: "Pending Approval",
            APPROVED: "Approved",
            REJECTED: "Rejected",
            PAID: "Paid",
            OVERDUE: "Overdue",
            OUT: "Out of Campus",
            RETURNED: "Returned Safety",
            OPEN: "Open Complaint",
            IN_PROGRESS: "In Progress",
            RESOLVED: "Resolved",
            COMPLETED: "Completed"
        }
    },
    ta: {
        title: "மாணவர் செயல்பாட்டு காலவரிசை",
        subtitle: "உங்கள் குழந்தையின் விடுப்புகள், கட்டணங்கள், புகார்கள் மற்றும் கேட் பதிவுகளின் நேரடித் தொகுப்பு.",
        langToggle: "English",
        loading: "செயல்பாட்டு காலவரிசை ஏற்றப்படுகிறது...",
        noEvents: "காலவரிசையில் இன்னும் எந்தச் செயல்பாடுகளும் பதிவு செய்யப்படவில்லை.",
        errorTitle: "காலவரிசையை ஏற்றுவதில் பிழை",
        errorDesc: "மாணவர் காலவரிசையைப் பெற முடியவில்லை. பின்னர் மீண்டும் முயற்சிக்கவும்.",
        timestamp: "தேதி மற்றும் நேரம்",
        type: "வகை",
        status: "நிலை",
        types: {
            LEAVE: "விடுப்பு விண்ணப்பம்",
            FEE: "கட்டண நிலை",
            COMPLAINT: "புகார் பதிவு",
            GATE_EXIT: "விடுதி வாயில் வெளியேற்றம்",
            GATE_ENTRY: "விடுதி வாயில் நுழைவு"
        },
        statuses: {
            PENDING: "அங்கீகாரத்திற்கு நிலுவையில் உள்ளது",
            APPROVED: "அங்கீகரிக்கப்பட்டது",
            REJECTED: "நிராகரிக்கப்பட்டது",
            PAID: "செலுத்தப்பட்டது",
            OVERDUE: "தவறியது",
            OUT: "வளாகத்திற்கு வெளியே",
            RETURNED: "பாதுகாப்பாக திரும்பினார்",
            OPEN: "தீர்க்கப்படாத புகார்",
            IN_PROGRESS: "செயல்பாட்டில் உள்ளது",
            RESOLVED: "தீர்க்கப்பட்டது",
            COMPLETED: "நிறைவடைந்தது"
        }
    }
};

export default function TrustTimeline() {
    const [lang, setLang] = useState('en');
    const t = translations[lang];

    const { data: timeline = [], isLoading, error } = useQuery({
        queryKey: ['parentTimeline'],
        queryFn: () => api.get('/parent/timeline').then(res => res.data),
        retry: false,
    });

    const toggleLanguage = () => {
        setLang(prev => (prev === 'en' ? 'ta' : 'en'));
    };

    const getStatusStyle = (status) => {
        const clean = (status || '').toUpperCase();
        if (['APPROVED', 'PAID', 'RESOLVED', 'RETURNED', 'COMPLETED'].includes(clean)) {
            return { bg: '#d1fae5', text: '#065f46', dot: '#10b981' };
        }
        if (['REJECTED', 'OVERDUE'].includes(clean)) {
            return { bg: '#fee2e2', text: '#991b1b', dot: '#ef4444' };
        }
        if (['PENDING', 'OPEN', 'IN_PROGRESS'].includes(clean)) {
            return { bg: '#fef3c7', text: '#92400e', dot: '#f59e0b' };
        }
        return { bg: '#e2e8f0', text: '#475569', dot: '#64748b' };
    };

    const getTypeIcon = (type) => {
        switch (type) {
            case 'LEAVE':
                return '📋';
            case 'FEE':
                return '💰';
            case 'COMPLAINT':
                return '🛠️';
            case 'GATE_EXIT':
                return '🚪🏃';
            case 'GATE_ENTRY':
                return '🚪✅';
            default:
                return '🔔';
        }
    };

    return (
        <DashboardLayout>
            {/* Header section with Language Toggle */}
            <div className="flex flex-col md:flex-row md:items-center justify-between pb-6 mb-8 border-b border-slate-100">
                <div>
                    <h1 className="text-2xl font-bold text-slate-800 tracking-tight">
                        {t.title}
                    </h1>
                    <p className="text-slate-500 text-sm mt-1">
                        {t.subtitle}
                    </p>
                </div>
                <button
                    onClick={toggleLanguage}
                    className="mt-4 md:mt-0 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white font-semibold rounded-lg text-sm transition shadow-sm cursor-pointer"
                >
                    🌐 {t.langToggle}
                </button>
            </div>

            {/* Error or Loading state */}
            {isLoading ? (
                <div className="flex flex-col items-center justify-center py-20">
                    <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-slate-800 mb-4"></div>
                    <p className="text-slate-500 font-medium text-sm">{t.loading}</p>
                </div>
            ) : error ? (
                <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-center max-w-xl mx-auto my-10">
                    <span className="text-4xl mb-4 block">⚠️</span>
                    <h3 className="text-lg font-bold text-red-800 mb-2">{t.errorTitle}</h3>
                    <p className="text-red-600 text-sm">{t.errorDesc}</p>
                </div>
            ) : timeline.length === 0 ? (
                <div className="bg-white border border-slate-100 rounded-xl p-12 text-center max-w-md mx-auto my-10 shadow-sm">
                    <span className="text-5xl mb-4 block">📜</span>
                    <p className="text-slate-500 font-medium">{t.noEvents}</p>
                </div>
            ) : (
                /* Timeline Component container */
                <div className="relative max-w-3xl mx-auto bg-white rounded-2xl border border-slate-100 p-8 shadow-sm">
                    {/* Vertical connecting line */}
                    <div className="absolute left-12 top-10 bottom-10 w-0.5 bg-slate-100"></div>

                    <div className="space-y-8">
                        {timeline.map((event, idx) => {
                            const styles = getStatusStyle(event.status);
                            const translatedType = t.types[event.type] || event.type;
                            const translatedStatus = t.statuses[event.status] || event.status;
                            const date = new Date(event.timestamp);
                            
                            return (
                                <div key={idx} className="relative flex items-start space-x-6">
                                    {/* Timeline Circle with Icon */}
                                    <div 
                                        className="relative z-10 flex items-center justify-center w-10 h-10 rounded-full border bg-white shadow-sm transition-transform hover:scale-105"
                                        style={{ borderColor: styles.dot }}
                                    >
                                        <span className="text-lg">{getTypeIcon(event.type)}</span>
                                    </div>

                                    {/* Content Card */}
                                    <div className="flex-1 bg-slate-50/50 hover:bg-slate-50 border border-slate-100 rounded-xl p-5 transition">
                                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-3">
                                            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                                                {translatedType}
                                            </span>
                                            
                                            {/* Status Badge */}
                                            <span 
                                                className="self-start text-[11px] font-bold px-2.5 py-1 rounded-full uppercase"
                                                style={{ backgroundColor: styles.bg, color: styles.text }}
                                            >
                                                {translatedStatus}
                                            </span>
                                        </div>

                                        <p className="text-[14px] font-medium text-slate-700 leading-relaxed">
                                            {event.description}
                                        </p>

                                        {/* Date display */}
                                        <div className="flex items-center space-x-2 mt-4 pt-3 border-t border-slate-100 text-xs text-slate-400">
                                            <span>📅</span>
                                            <span>
                                                {date.toLocaleDateString(lang === 'ta' ? 'ta-IN' : 'en-US', {
                                                    weekday: 'short',
                                                    year: 'numeric',
                                                    month: 'short',
                                                    day: 'numeric'
                                                })}
                                            </span>
                                            <span className="mx-2">•</span>
                                            <span>🕒</span>
                                            <span>
                                                {date.toLocaleTimeString(lang === 'ta' ? 'ta-IN' : 'en-US', {
                                                    hour: '2-digit',
                                                    minute: '2-digit'
                                                })}
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>
            )}
        </DashboardLayout>
    );
}
