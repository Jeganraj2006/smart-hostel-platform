import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import api from '../../services/api';

const getPreferencesOrDefault = (user) => {
  return user.roommatePreferences || {
    sleepSchedule: 'EARLY_BIRD',
    cleanlinessLevel: 3,
    studyHabit: 'SILENT',
    preferredLanguage: 'English'
  };
};

const calculatePairScore = (s1, s2) => {
  const p1 = getPreferencesOrDefault(s1);
  const p2 = getPreferencesOrDefault(s2);
  let score = 0;

  if (p1.sleepSchedule && p1.sleepSchedule === p2.sleepSchedule) {
    score += 3;
  }
  if (p1.cleanlinessLevel !== undefined && p2.cleanlinessLevel !== undefined) {
    if (Math.abs(p1.cleanlinessLevel - p2.cleanlinessLevel) <= 1) {
      score += 2;
    }
  }
  if (p1.studyHabit && p1.studyHabit === p2.studyHabit) {
    score += 2;
  }
  if (p1.preferredLanguage && p2.preferredLanguage && p1.preferredLanguage.toLowerCase() === p2.preferredLanguage.toLowerCase()) {
    score += 1;
  }
  return score;
};

const getRoomCompatibilityScore = (students) => {
  if (!students || students.length <= 1) return 0;
  let total = 0;
  let pairs = 0;
  for (let i = 0; i < students.length; i++) {
    for (let j = i + 1; j < students.length; j++) {
      total += calculatePairScore(students[i], students[j]);
      pairs++;
    }
  }
  return Math.round(total / pairs);
};

export default function RoomAllocation() {
  const [applyingId, setApplyingId] = useState(null);

  const { data: suggestions = [], isLoading, refetch } = useQuery({
    queryKey: ['allocationSuggestions'],
    queryFn: () => api.get('/rooms/allocation-suggestions').then(res => res.data),
    retry: false
  });

  const handleApply = async (roomId, studentIds) => {
    setApplyingId(roomId);
    try {
      await api.post('/rooms/allocation-suggestions/apply', [{ roomId, studentIds }]);
      toast.success('Room allocation applied successfully!');
      refetch();
    } catch (err) {
      toast.error(err.response?.data?.error || err.response?.data || 'Failed to apply allocation');
    } finally {
      setApplyingId(null);
    }
  };

  return (
    <DashboardLayout>
      <div className="p-6 max-w-6xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-slate-800">Smart Room Allocation suggestions</h1>
          <p className="text-slate-500 mt-2 text-sm">
            AI-driven compatibility matches using roommate schedules, cleanliness metrics, study patterns, and languages.
          </p>
        </div>

        {isLoading ? (
          <div className="flex justify-center items-center py-12">
            <span className="text-slate-400 text-sm animate-pulse">Computing best allocations...</span>
          </div>
        ) : suggestions.length === 0 ? (
          <div className="bg-white border border-slate-200 rounded-xl p-8 text-center text-slate-400">
            <span className="text-4xl block mb-2">📋</span>
            No pending suggestions. All approved students are successfully allocated to rooms.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {suggestions.map((sug) => {
              const score = getRoomCompatibilityScore(sug.suggestedStudents);
              const isApplying = applyingId === sug.roomId;

              return (
                <div key={sug.roomId} className="bg-white border border-slate-100 rounded-xl shadow-xs p-6 hover:shadow-md transition-shadow flex flex-col justify-between">
                  <div>
                    <div className="flex justify-between items-center mb-6">
                      <div>
                        <h2 className="text-lg font-bold text-slate-800">Room {sug.roomNumber}</h2>
                        <p className="text-xs text-slate-400 uppercase font-semibold mt-0.5">{sug.blockName}</p>
                      </div>
                      {sug.suggestedStudents.length > 1 ? (
                        <span className="bg-indigo-50 text-indigo-700 text-xs font-semibold px-2.5 py-1 rounded-full border border-indigo-100">
                          Score: +{score}
                        </span>
                      ) : (
                        <span className="bg-slate-50 text-slate-600 text-xs font-semibold px-2.5 py-1 rounded-full border border-slate-100">
                          Single Space
                        </span>
                      )}
                    </div>

                    <div className="space-y-4 mb-6">
                      {sug.suggestedStudents.map((st) => {
                        const prefs = getPreferencesOrDefault(st);
                        return (
                          <div key={st.id} className="bg-slate-50 border border-slate-100 rounded-lg p-4">
                            <div className="flex justify-between items-start mb-2">
                              <div>
                                <p className="text-sm font-semibold text-slate-700">{st.name}</p>
                                <p className="text-xs text-slate-400">{st.email}</p>
                              </div>
                            </div>
                            <div className="flex flex-wrap gap-1.5 mt-3">
                              <span className="bg-blue-50 text-blue-700 text-[10px] font-semibold px-2 py-0.5 rounded border border-blue-100">
                                {prefs.sleepSchedule === 'EARLY_BIRD' ? '🌅 Early Bird' : '🦉 Night Owl'}
                              </span>
                              <span className="bg-emerald-50 text-emerald-700 text-[10px] font-semibold px-2 py-0.5 rounded border border-emerald-100">
                                🧼 Clean: {prefs.cleanlinessLevel}/5
                              </span>
                              <span className="bg-amber-50 text-amber-700 text-[10px] font-semibold px-2 py-0.5 rounded border border-amber-100">
                                📚 {prefs.studyHabit === 'SILENT' ? 'Silent Study' : prefs.studyHabit === 'MUSIC_OK' ? 'Music OK' : 'Group Study'}
                              </span>
                              <span className="bg-purple-50 text-purple-700 text-[10px] font-semibold px-2 py-0.5 rounded border border-purple-100">
                                🗣️ {prefs.preferredLanguage}
                              </span>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>

                  <button
                    onClick={() => handleApply(sug.roomId, sug.suggestedStudents.map(st => st.id))}
                    disabled={isApplying}
                    className="w-full bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold py-2.5 px-4 rounded-lg transition-colors cursor-pointer disabled:bg-blue-300 disabled:cursor-not-allowed text-center"
                  >
                    {isApplying ? 'Applying...' : 'Apply Allocation'}
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
