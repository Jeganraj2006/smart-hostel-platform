import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';
import DashboardLayout from '../../components/layout/DashboardLayout';
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    ResponsiveContainer,
    LineChart,
    Line
} from 'recharts';

export default function Analytics() {
    // 1. Fetch Occupancy data
    const { data: occupancy = [], isLoading: loadingOccupancy, error: errorOccupancy } = useQuery({
        queryKey: ['analyticsOccupancy'],
        queryFn: () => api.get('/analytics/occupancy').then(res => res.data),
    });

    // 2. Fetch Complaint Heatmap data
    const { data: complaints = [], isLoading: loadingComplaints, error: errorComplaints } = useQuery({
        queryKey: ['analyticsComplaints'],
        queryFn: () => api.get('/analytics/complaint-heatmap').then(res => res.data),
    });

    // 3. Fetch Leave Patterns data
    const { data: leaves = {}, isLoading: loadingLeaves, error: errorLeaves } = useQuery({
        queryKey: ['analyticsLeaves'],
        queryFn: () => api.get('/analytics/leave-patterns').then(res => res.data),
    });

    // 4. Fetch Fee Forecast data
    const { data: forecast = {}, isLoading: loadingForecast, error: errorForecast } = useQuery({
        queryKey: ['analyticsForecast'],
        queryFn: () => api.get('/analytics/fee-forecast').then(res => res.data),
    });

    // 5. Fetch Resource Consumption summary data
    const { data: resources = [], isLoading: loadingResources, error: errorResources } = useQuery({
        queryKey: ['analyticsResources'],
        queryFn: () => api.get('/resources/summary').then(res => res.data),
    });

    // Transform Leave patterns to recharts structure
    const dayOrder = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
    const leaveTimelineData = dayOrder.map(d => ({
        day: d.charAt(0) + d.slice(1).toLowerCase(),
        count: leaves?.byDayOfWeek?.[d] || 0
    }));

    // Transform Heatmap data to block -> category grid
    const blocks = Array.from(new Set(complaints.map(c => c.blockName))).sort();
    const categories = Array.from(new Set(complaints.map(c => c.category))).sort();

    const getComplaintCount = (block, cat) => {
        const match = complaints.find(c => c.blockName === block && c.category === cat);
        return match ? match.count : 0;
    };

    const getHeatmapColor = (count) => {
        if (count === 0) return { bg: '#f8fafc', text: '#94a3b8' }; // slate-50
        if (count <= 2) return { bg: '#ffedd5', text: '#c2410c' };  // orange-100
        if (count <= 5) return { bg: '#fed7aa', text: '#9a3412' };  // orange-200
        return { bg: '#fecaca', text: '#991b1b' };                 // red-200
    };

    const formatCurrency = (val) => {
        return new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency: 'INR',
            maximumFractionDigits: 0
        }).format(val || 0);
    };

    const isPageLoading = loadingOccupancy || loadingComplaints || loadingLeaves || loadingForecast || loadingResources;

    const currentMonthStr = new Date().toISOString().substring(0, 7); // "YYYY-MM"
    const getLatestMonthStr = () => {
        if (resources.length === 0) return null;
        const months = Array.from(new Set(resources.map(r => r.month))).sort().reverse();
        return months[0];
    };
    const activeMonth = getLatestMonthStr() || currentMonthStr;
    const activeMonthResources = resources.filter(r => r.month === activeMonth);
    const hasError = errorOccupancy || errorComplaints || errorLeaves || errorForecast || errorResources;
    const errorMsg = errorOccupancy?.message || errorComplaints?.message || errorLeaves?.message || errorForecast?.message || errorResources?.message;

    return (
        <DashboardLayout>
            <div className="mb-6">
                <h1 className="text-2xl font-bold text-slate-800 tracking-tight">System Analytics & Forecasting</h1>
                <p className="text-slate-500 text-sm mt-1">Real-time room occupancy, complaint hot-spots, leave patterns, and financial forecasting.</p>
            </div>

            {hasError ? (
                <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-center max-w-xl mx-auto my-10">
                    <span className="text-4xl mb-4 block">⚠️</span>
                    <h3 className="text-lg font-bold text-red-800 mb-2">Error Loading Analytics</h3>
                    <p className="text-red-600 text-sm">{errorMsg || 'Failed to load system reports. Please try again.'}</p>
                </div>
            ) : isPageLoading ? (
                <div className="flex flex-col items-center justify-center py-20">
                    <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-slate-800 mb-4"></div>
                    <p className="text-slate-500 font-medium text-sm">Loading analytics dashboards...</p>
                </div>
            ) : (
                <div className="space-y-6">
                    
                    {/* Financial Forecast Widget */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <div className="bg-white border border-slate-100 rounded-xl p-5 shadow-sm">
                            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block">Pending Amount (Next 30 Days)</span>
                            <h3 className="text-2xl font-bold text-slate-800 mt-2">
                                {formatCurrency(forecast.pendingAmountNext30Days)}
                            </h3>
                            <p className="text-xs text-slate-500 mt-1">Total pending fees due within 30 days</p>
                        </div>
                        <div className="bg-white border border-slate-100 rounded-xl p-5 shadow-sm">
                            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block">Historical Collection Rate</span>
                            <h3 className="text-2xl font-bold text-amber-600 mt-2">
                                {((forecast.historicalOnTimeRate || 0) * 100).toFixed(1)}%
                            </h3>
                            <p className="text-xs text-slate-500 mt-1">On-time payments based on historical trends</p>
                        </div>
                        <div className="bg-gradient-to-br from-emerald-50 to-emerald-100/50 border border-emerald-200/60 rounded-xl p-5 shadow-sm">
                            <span className="text-xs font-bold text-emerald-800 uppercase tracking-wider block">Projected Collection</span>
                            <h3 className="text-2xl font-bold text-emerald-700 mt-2">
                                {formatCurrency(forecast.projectedCollection)}
                            </h3>
                            <p className="text-xs text-emerald-800/80 mt-1">Weighted forecasted collection amount</p>
                        </div>
                    </div>

                    {/* Occupancy and Leave patterns Row */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        
                        {/* Occupancy Chart */}
                        <div className="bg-white border border-slate-100 rounded-xl p-5 shadow-sm">
                            <h3 className="text-base font-bold text-slate-800 mb-4">🏠 Room Occupancy per Block</h3>
                            <div style={{ width: '100%', height: 300 }}>
                                <ResponsiveContainer>
                                    <BarChart data={occupancy} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                                        <XAxis dataKey="blockName" stroke="#94a3b8" fontSize={12} />
                                        <YAxis stroke="#94a3b8" fontSize={12} />
                                        <Tooltip cursor={{ fill: '#f8fafc' }} />
                                        <Legend wrapperStyle={{ fontSize: 12, paddingTop: 10 }} />
                                        <Bar dataKey="filled" name="Filled Beds" fill="#10b981" radius={[4, 4, 0, 0]} />
                                        <Bar dataKey="available" name="Available Beds" fill="#f59e0b" radius={[4, 4, 0, 0]} />
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                        </div>

                        {/* Leave Patterns Chart */}
                        <div className="bg-white border border-slate-100 rounded-xl p-5 shadow-sm">
                            <h3 className="text-base font-bold text-slate-800 mb-4">📈 Weekly Outpass Leave Spikes</h3>
                            <div style={{ width: '100%', height: 300 }}>
                                <ResponsiveContainer>
                                    <LineChart data={leaveTimelineData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                                        <XAxis dataKey="day" stroke="#94a3b8" fontSize={12} />
                                        <YAxis stroke="#94a3b8" fontSize={12} />
                                        <Tooltip />
                                        <Legend wrapperStyle={{ fontSize: 12, paddingTop: 10 }} />
                                        <Line type="monotone" dataKey="count" name="Applied Leaves" stroke="#6366f1" strokeWidth={2.5} activeDot={{ r: 6 }} />
                                    </LineChart>
                                </ResponsiveContainer>
                            </div>
                        </div>
                    </div>

                    {/* Leave types breakdown & Complaint Heatmap Row */}
                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                        
                        {/* Leave Types Sidebar */}
                        <div className="bg-white border border-slate-100 rounded-xl p-5 shadow-sm flex flex-col justify-between">
                            <div>
                                <h3 className="text-base font-bold text-slate-800 mb-4">📋 Leaves by Type</h3>
                                <div className="space-y-3">
                                    {Object.entries(leaves?.byLeaveType || {}).map(([type, val]) => (
                                        <div key={type} className="flex justify-between items-center bg-slate-50 rounded-lg p-3 border border-slate-100">
                                            <span className="text-sm font-semibold text-slate-600 tracking-wide">{type}</span>
                                            <span className="text-xs font-bold px-2.5 py-1 bg-slate-200 text-slate-700 rounded-full">{val}</span>
                                        </div>
                                    ))}
                                    {Object.keys(leaves?.byLeaveType || {}).length === 0 && (
                                        <p className="text-slate-400 text-sm text-center py-6">No leave applications recorded.</p>
                                    )}
                                </div>
                            </div>
                        </div>

                        {/* Complaint Heatmap Grid */}
                        <div className="lg:col-span-2 bg-white border border-slate-100 rounded-xl p-5 shadow-sm">
                            <h3 className="text-base font-bold text-slate-800 mb-4">🔥 Complaint Heat-Spots (Last 90 Days)</h3>
                            {complaints.length === 0 ? (
                                <p className="text-slate-400 text-sm text-center py-10">No complaints registered in the last 90 days.</p>
                            ) : (
                                <div className="overflow-x-auto">
                                    <table className="min-w-full border-collapse">
                                        <thead>
                                            <tr>
                                                <th className="border-b border-slate-100 text-left p-2.5 text-xs font-bold uppercase tracking-wider text-slate-400">Block</th>
                                                {categories.map(cat => (
                                                    <th key={cat} className="border-b border-slate-100 text-center p-2.5 text-xs font-bold uppercase tracking-wider text-slate-400">
                                                        {cat}
                                                    </th>
                                                ))}
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {blocks.map(block => (
                                                <tr key={block} className="hover:bg-slate-50/50">
                                                    <td className="p-2.5 font-bold text-slate-700 text-sm border-b border-slate-100">{block}</td>
                                                    {categories.map(cat => {
                                                        const count = getComplaintCount(block, cat);
                                                        const color = getHeatmapColor(count);
                                                        return (
                                                            <td 
                                                                key={cat} 
                                                                className="text-center p-2.5 border-b border-slate-100 text-sm font-semibold transition"
                                                                style={{ backgroundColor: color.bg, color: color.text }}
                                                            >
                                                                {count}
                                                            </td>
                                                        );
                                                    })}
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Resource Consumption Panel */}
                    <div className="bg-white border border-slate-100 rounded-xl p-5 shadow-sm">
                        <h3 className="text-base font-bold text-slate-800 mb-4">
                            🔌 Resource Consumption Summary ({activeMonth})
                        </h3>
                        {activeMonthResources.length === 0 ? (
                            <p className="text-slate-400 text-sm text-center py-6">No resource logs registered for this month.</p>
                        ) : (
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                                {['ELECTRICITY', 'WATER', 'MESS_WASTAGE'].map(type => {
                                    const Icon = type === 'ELECTRICITY' ? '⚡' : type === 'WATER' ? '💧' : '🍲';
                                    const Label = type === 'ELECTRICITY' ? 'Electricity' : type === 'WATER' ? 'Water' : 'Mess Wastage';
                                    const Color = type === 'ELECTRICITY' ? 'text-amber-500 bg-amber-50' : type === 'WATER' ? 'text-blue-500 bg-blue-50' : 'text-rose-500 bg-rose-50';
                                    
                                    const typeLogs = activeMonthResources.filter(r => r.resourceType === type);

                                    return (
                                        <div key={type} className="border border-slate-100 rounded-xl p-4 bg-slate-50/50">
                                            <div className="flex items-center space-x-3 mb-3">
                                                <span className={`text-xl p-2 rounded-lg ${Color}`}>{Icon}</span>
                                                <span className="font-bold text-slate-700 text-sm">{Label}</span>
                                            </div>
                                            <div className="space-y-2">
                                                {typeLogs.map((log, lIdx) => (
                                                    <div key={lIdx} className="flex justify-between items-center text-xs">
                                                        <span className="font-semibold text-slate-500">Block {log.blockName}</span>
                                                        <span className="font-bold text-slate-800">
                                                            {log.totalQuantity.toFixed(1)} {log.unit}
                                                        </span>
                                                    </div>
                                                ))}
                                                {typeLogs.length === 0 && (
                                                    <p className="text-slate-400 text-xs py-2">No logs recorded.</p>
                                                )}
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>
                </div>
            )}
        </DashboardLayout>
    );
}
