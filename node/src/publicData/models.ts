// 공개 데이터 모델 (필드 대응: spec/PUBLIC_DATA.md). extra 키는 학교 원문 그대로
export type ExtraFields = Record<string, string>;

export interface OrganizationCategory { code: string; name: string; nameEn: string; extra: ExtraFields }
export interface Term { year: string; semesterCode: string; code: string; name: string; extra: ExtraFields }
export interface Department {
  code: string; level: string; collegeCode: string; collegeName: string; unitCode: string; unitName: string;
  majorCode: string; majorName: string; nameEn: string; extra: ExtraFields;
}
export interface CourseType { code: string; name: string; categoryCode: string; extra: ExtraFields }
export interface SelectiveDomain { code: string; name: string; nameEn: string; extra: ExtraFields }
export interface Lecture {
  courseNumber: string; section: string; name: string; courseTypeCode: string; courseTypeName: string;
  domainCode: string; domainName: string; credits: string; hours: string; totalHours: string; schedule: string;
  room: string; instructor: string; gradeLevel: string; languageCode: string; languageName: string;
  departmentCode: string; departmentName: string; managingDepartment: string; remark: string;
  year: string; semesterCode: string; extra: ExtraFields;
}
export interface CurriculumCourse {
  departmentCode: string; departmentName: string; categoryCode: string; year: string; courseNumber: string; name: string;
  courseTypeCode: string; courseTypeName: string; credits: string; lectureHours: string; practiceHours: string; totalHours: string;
  gradeLevel: string; semesterCode: string; semesterName: string; overview: string; extra: ExtraFields;
}
export interface CalendarEvent {
  year: string; month: string; categoryCode: string; categoryName: string; title: string; titleEn: string;
  startDate: string; endDate: string; extra: ExtraFields;
}
export interface DormitoryMenuDay { date: string; breakfast: string; lunch: string; dinner: string; alternative: string }
export interface DormitoryNotice { id: string; title: string; date: string; views: number; fileName: string }
export interface DormitoryNotices { total: number; pinned: DormitoryNotice[]; items: DormitoryNotice[] }
export interface CafeteriaMenuItem {
  buildingName: string; location: string; placeId: string; placeName: string; date: string; day: string;
  mealTypeId: string; mealName: string; menuName: string; price: string;
}
export interface CafeteriaPlace { placeId: string; placeName: string; buildingName: string; location: string; extra: ExtraFields }
export interface CampusPlace {
  id: string; name: string; nameEn: string; placeType: string; category: string; latitude: number; longitude: number;
  info: string; phone: string; description: string; extra: ExtraFields;
}
export interface ExtracurricularEvent {
  id: string; title: string; start: string; end: string; categoryTitle: string; url: string; description: string; location: string;
}
