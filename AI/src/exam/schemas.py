from pydantic import BaseModel, Field
from typing import List, Optional

class OptionSchema(BaseModel):
    option_key: int = Field(..., description="Số thứ tự đáp án (1, 2, 3, 4)")
    option_text: Optional[str] = None
    option_image_url: Optional[str] = None
    is_correct: bool = False

class PassageSchema(BaseModel):
    title: Optional[str] = "Bài đọc"
    passage_type: str = Field(default="TEXT", description="TEXT | IMAGE_POSTER | EMAIL | NOTICE")
    content_jp: Optional[str] = None
    image_url: Optional[str] = None

class QuestionSchema(BaseModel):
    question_number: int
    mondai_code: str
    question_text: str
    image_url: Optional[str] = None
    audio_start_time: Optional[int] = None
    audio_end_time: Optional[int] = None
    transcript_jp: Optional[str] = None
    transcript_vi: Optional[str] = None
    star_position: Optional[int] = None
    has_visual_options: bool = False
    explanation_vi: Optional[str] = None
    passage: Optional[PassageSchema] = None
    options: List[OptionSchema] = []

class SectionSchema(BaseModel):
    session_type: str = Field(..., description="VOCABULARY | GRAMMAR_READING | LISTENING")
    section_title: str
    audio_url: Optional[str] = None
    order_index: int = 1
    duration_minutes: Optional[int] = 30
    questions: List[QuestionSchema] = []

class ExamExtractedResponse(BaseModel):
    title: str
    jlpt_level: str = "N3"
    exam_type: str = "JLPT_OFFICIAL_MOCK"
    total_duration_minutes: int = 140
    sections: List[SectionSchema] = []
