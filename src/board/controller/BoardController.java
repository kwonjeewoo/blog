package board.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

import board.dao.BoardDAO;
import board.dto.BoardCommentDTO;
import board.dto.BoardDTO;

@WebServlet("/board_servlet/*")
public class BoardController extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
    public BoardController() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String url = request.getRequestURL().toString();
		BoardDAO dao = new BoardDAO();
		
		if(url.indexOf("list.do") != -1) 
		{
			List<BoardDTO> list = dao.list(); // 게시물 목록이 넘어옴
			// 출력페이지에서 사용할 수 있도록 request 영역에 저장
			request.setAttribute("list", list);
			// 출력 페이지로 이동
			String page = "/board/board_list.jsp";
			RequestDispatcher rd = request.getRequestDispatcher(page);
			rd.forward(request, response);
		}
		else if(url.indexOf("insert.do") != -1) 
		{
			BoardDTO dto = new BoardDTO();
			
			// MultipartRequest 객체를 생성하는 순간 기존에 사용하던 request 는 사용할 수 없다
			String writer = request.getParameter("writer");
			String subject = request.getParameter("subject");
			String content = request.getParameter("content");
			String password = request.getParameter("password");
			String ip = request.getRemoteAddr(); // 클라이언트의 ip주소
			dto.setWriter(writer);
			dto.setSubject(subject);
			dto.setContent(content);
			dto.setPassword(password);
			dto.setIp(ip);
			
			System.out.println(dto);

			dao.insert(dto); // 레코드가 추가됨
			// 게시물 목록 갱신
			String page = 
					request.getContextPath() + "/board_servlet/list.do";
			response.sendRedirect(page);
		}
		
		else if(url.indexOf("view.do") != -1) 
		{
			int num = Integer.parseInt(request.getParameter("num"));
			// 조회수 증가 처리
			dao.plusReadCount(num, request.getSession());
			
			// view 페이지의 경우 줄바꿈 처리를 해줘야 하기때문에 true 를 전달
			BoardDTO dto = dao.view(num, true);
			System.out.println("상세화면 dto" + dto);
			// 출력을 위해 request 영역에 저장
			request.setAttribute("dto", dto);
			//출력 페이지로 이동
			String page = "/board/board_view.jsp";
			RequestDispatcher rd = request.getRequestDispatcher(page);
			rd.forward(request, response);
			
		}
		else if(url.indexOf("pass_check.do") != -1) 
		{
			int num = Integer.parseInt(request.getParameter("num"));
			String password = request.getParameter("password");
			// 올바른 비밀번호인지 확인
			String result = dao.passwordCheck(num, password);
			System.out.println("비밀번호 체크결과" + result);
			String page = new String();
			// 비번이 맞으면 수정 화면으로 이동
			if(result != null) 
			{
				page = "/board/board_edit.jsp";
				// edit 페이지의 경우 줄바꿈 처리가 필요없음으로 false 를 전달
				request.setAttribute("dto", dao.view(num, false));
				RequestDispatcher rd = request.getRequestDispatcher(page);
				rd.forward(request, response);
			}
			else 
			{
				page = request.getContextPath() + 
						"/board_servlet/view.do?num="+num+"&message=error";
				response.sendRedirect(page);
			}
			
			// 비번이 틀리면 되돌아감
		}
		else if(url.indexOf("update.do") != -1) 
		{
			// 폼에서 입력한 값을 dto에 저장
			BoardDTO dto = new BoardDTO();
			int num = Integer.parseInt(request.getParameter("num"));
			String writer = request.getParameter("writer");
			String subject = request.getParameter("subject");
			String content = request.getParameter("content");
			String password = request.getParameter("password");
			String ip = request.getRemoteAddr(); // 클라이언트의 ip주소
			
			dto.setNum(num);
			dto.setWriter(writer);
			dto.setSubject(subject);
			dto.setContent(content);
			dto.setPassword(password);
			dto.setIp(ip);
			
			
			// 
			String result = dao.passwordCheck(num, password);
			if(result != null) 
			{ // 비밀번호가 맞을 경우
				// dao에 update 요청
				dao.update(dto);
				// list.do로 이동
				String page = request.getContextPath() + "/board_servlet/list.do";
				response.sendRedirect(page);
			} 
			else 
			{
				request.setAttribute("dto", dto);
				String page = "/board/board_edit.jsp?password_error=y";
				RequestDispatcher rd = request.getRequestDispatcher(page);
				rd.forward(request, response);
			}
		}
		else if(url.indexOf("delete.do") != -1)
		{
			// enctype="multipart/form-data" 로 넘어온 값은 
			// request 객체로 받을 수 없다.
			
			int num = Integer.parseInt(request.getParameter("num"));
			dao.delete(num);
			// DB 테이블 칼럽 추가 
			// alter table board add shows char(1) default('y');
			// 이럴경우 데이터를 완전 삭제가 아닌 shows 값을 n 으로 변경함으로 view 상에는 보이진 않지만
			// 데이터는 존재하는 형태로 진행합니다.
			
			String page = new String();
			page = request.getContextPath() + 
					"/board_servlet/list.do";
			response.sendRedirect(page);
		}
		else if(url.indexOf("commentList.do") != -1)
		{
			int num = Integer.parseInt(request.getParameter("num"));
			
			// 댓글 목록이 list로 넘어옴
			List<BoardCommentDTO> list = dao.commentList(num);
			// 출력 페이지에서 읽을 수 있도록 request 영역에 저장
			request.setAttribute("list", list);
			
			// 화면 전환
			String page = "/board/comment_list.jsp";
			RequestDispatcher rd = request.getRequestDispatcher(page);
			rd.forward(request, response);
		}
		else if(url.indexOf("commentAdd.do") != -1)
		{
			BoardCommentDTO dto = new BoardCommentDTO();
			int board_num = Integer.parseInt(request.getParameter("board_num"));
			String writer = request.getParameter("writer");
			String content = request.getParameter("content");
			
			dto.setBoard_num(board_num);
			dto.setWriter(writer);
			dto.setContent(content);
			
			System.out.println(dto);
		
			// 레코드가 추가됨
			dao.commentAdd(dto);
			// 실행이 끝나면 view.jsp의 콜백함수(success)로 넘어감
		}
		else if(url.indexOf("reply.do") != -1) 
		{
			// 게시물 번호 조회
			int num = Integer.parseInt(request.getParameter("num"));
			// 게시물 내용을 dto로 받음
			BoardDTO dto = dao.view(num, false);
			// 답변 작성의 편의를 위해 reply.jsp 페이지에 dto를 전달
			dto.setContent("---게시물의 내용---\n" + dto.getContent());
			request.setAttribute("dto", dto);
			String page = "/board/reply.jsp";
			RequestDispatcher rd = request.getRequestDispatcher(page);
			rd.forward(request, response);
		}
		else if(url.indexOf("insertReply.do") != -1) 
		{
			int num = Integer.parseInt(request.getParameter("num"));
			// 원글 내용
			BoardDTO dto = dao.view(num, false);
			int ref = dto.getref(); // 답변 그룹번호
			int re_step = dto.getRe_step() + 1; // 출력 순번
			int re_level = dto.getRe_level() + 1; // 답변 단계
			
			// 답변 내용
			String writer = request.getParameter("writer");
			String subject = request.getParameter("subject");
			String content = request.getParameter("content");
			String password = request.getParameter("password");
			
			dto.setWriter(writer);
			dto.setSubject(subject);
			dto.setContent(content);
			dto.setPassword(password);
			dto.setref(ref);
			dto.setRe_level(re_level);
			dto.setRe_step(re_step);
			// 답글 순서 조정
			dao.updateStep(ref, re_step);
			// 답글 쓰기
			dao.reply(dto);
			// 목록으로 이동
			String page = "/board_servlet/list.do";
			response.sendRedirect(request.getContextPath() + page);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
