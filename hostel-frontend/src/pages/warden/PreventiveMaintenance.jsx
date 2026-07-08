import { useEffect, useState } from 'react';
import { complaintService } from '../../services/complaintService';
import toast from 'react-hot-toast';
import { 
  FaWrench, 
  FaExclamationTriangle, 
  FaCheckCircle, 
  FaClock 
} from 'react-icons/fa';

const categoryColors = {
  ELECTRICAL: 'bg-red-500/10 text-red-400 border-red-500/20',
  PLUMBING: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  CLEANLINESS: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  FURNITURE: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  INTERNET: 'bg-purple-500/10 text-purple-400 border-purple-500/20',
  OTHER: 'bg-slate-500/10 text-slate-400 border-slate-500/20',
};

export default function PreventiveMaintenance() {
  const [flags, setFlags] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchFlags = async (showLoading = false) => {
    try {
      if (showLoading) setLoading(true);
      setError(null);
      const res = await complaintService.getPreventiveFlags();
      setFlags(res.data);
    } catch (err) {
      setError(err);
      toast.error('Failed to load preventive maintenance flags');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchFlags(false);
  }, []);

  const handleResolve = async (id) => {
    try {
      await complaintService.resolvePreventiveFlag(id);
      toast.success('Maintenance scheduled! Flag acknowledged.');
      fetchFlags(true);
    } catch {
      toast.error('Failed to update flag');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-white flex items-center gap-3">
          <FaWrench className="h-8 w-8 text-teal-400" />
          Preventive Maintenance
        </h1>
        <p className="mt-2 text-sm text-slate-400">
          Monitor recurring complaints flagged automatically by the NLP pipeline for pro-active maintenance scheduling.
        </p>
      </div>

      {error ? (
        <div className="bg-red-500/10 border border-red-500/20 rounded-xl p-6 text-center max-w-xl mx-auto my-10 text-red-400">
            <span className="text-4xl mb-4 block">⚠️</span>
            <h3 className="text-lg font-bold mb-2">Failed to Load Maintenance Flags</h3>
            <p className="text-sm">{error.message || 'Please check your connection.'}</p>
        </div>
      ) : loading ? (
        <div className="flex flex-col items-center justify-center py-20">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-teal-400 mb-4"></div>
          <p className="text-slate-400 text-sm">Loading maintenance flags...</p>
        </div>
      ) : flags.length === 0 ? (
        <div className="text-center py-16 bg-slate-900/40 backdrop-blur-md rounded-2xl border border-slate-800">
          <FaCheckCircle className="h-16 w-16 text-teal-500/30 mx-auto mb-4" />
          <h3 className="text-xl font-medium text-slate-300">All systems operational</h3>
          <p className="text-sm text-slate-500 mt-1">No recurring assets flagged for maintenance at this time.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {flags.map((flag) => (
            <div 
              key={flag.id} 
              className="bg-slate-900/60 backdrop-blur-md border border-slate-800 hover:border-slate-700/80 rounded-2xl p-6 transition-all duration-300 flex flex-col justify-between shadow-lg"
            >
              <div>
                <div className="flex justify-between items-start gap-2 mb-4">
                  <span className="text-sm font-semibold tracking-wide text-slate-400 uppercase">
                    Asset ID: <strong className="text-white text-base tracking-normal normal-case">{flag.assetId}</strong>
                  </span>
                  <span className={`px-2.5 py-1 text-xs font-semibold rounded-full border ${categoryColors[flag.category] || categoryColors.OTHER}`}>
                    {flag.category}
                  </span>
                </div>

                <div className="flex items-center gap-3 p-4 bg-amber-500/10 border border-amber-500/20 rounded-xl mb-4">
                  <FaExclamationTriangle className="h-6 w-6 text-amber-500 flex-shrink-0 animate-pulse" />
                  <div>
                    <span className="text-sm font-semibold text-amber-400">Recurrence Threshold Met</span>
                    <p className="text-xs text-slate-300 mt-0.5">
                      {flag.complaintCount} complaints reported on this asset within 60 days.
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2 text-xs text-slate-500 mb-6">
                  <FaClock className="h-4 w-4" />
                  <span>Flagged: {new Date(flag.flaggedAt).toLocaleString()}</span>
                </div>
              </div>

              <button
                onClick={() => handleResolve(flag.id)}
                className="w-full py-3 px-4 bg-gradient-to-r from-teal-500 to-emerald-600 hover:from-teal-600 hover:to-emerald-700 text-white font-medium rounded-xl shadow-lg hover:shadow-teal-500/20 active:scale-[0.98] transition-all duration-200"
              >
                Schedule Maintenance
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
