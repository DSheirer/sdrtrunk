cmake_minimum_required(VERSION 3.18)
project(hydrasdr_jni LANGUAGES C)

include(FetchContent)

# Find JNI headers
find_package(JNI REQUIRED)

# ============================================================
# Find or fetch libhydrasdr
# ============================================================
# Priority order:
#   1. System-installed libhydrasdr (find_package)
#   2. Local source tree (HYDRASDR_DIR)
#   3. Auto-clone from official GitHub repo (FetchContent)
# ============================================================

# Option to force FetchContent even if a local copy exists
option(HYDRASDR_FETCH "Always clone libhydrasdr from GitHub" OFF)

# Optional local override path
set(HYDRASDR_DIR "" CACHE PATH
	"Path to local hydrasdr-host source tree or install prefix (optional)")

set(HYDRASDR_USE_FETCH OFF)
set(HYDRASDR_USE_LOCAL OFF)

if(NOT HYDRASDR_FETCH)
	# 1. Try system-installed
	find_package(HydraSDR QUIET)

	if(NOT HydraSDR_FOUND AND HYDRASDR_DIR)
		# 2. Try local source tree
		find_path(HYDRASDR_INCLUDE_DIR
			NAMES hydrasdr.h
			PATHS
				${HYDRASDR_DIR}/libhydrasdr/src
				${HYDRASDR_DIR}/include
				/usr/local/include
				/usr/include
			NO_DEFAULT_PATH
		)

		find_library(HYDRASDR_LIBRARY
			NAMES hydrasdr
			PATHS
				${HYDRASDR_DIR}/build/libhydrasdr/src
				${HYDRASDR_DIR}/build
				${HYDRASDR_DIR}/lib
				/usr/local/lib
				/usr/lib
			NO_DEFAULT_PATH
		)

		if(HYDRASDR_INCLUDE_DIR AND HYDRASDR_LIBRARY)
			set(HYDRASDR_USE_LOCAL ON)
			message(STATUS "Using local libhydrasdr:")
			message(STATUS "  Header:  ${HYDRASDR_INCLUDE_DIR}")
			message(STATUS "  Library: ${HYDRASDR_LIBRARY}")
		endif()
	endif()

	if(NOT HydraSDR_FOUND AND NOT HYDRASDR_USE_LOCAL)
		# Also try system paths without HYDRASDR_DIR
		find_path(HYDRASDR_INCLUDE_DIR NAMES hydrasdr.h
			PATHS /usr/local/include /usr/include)
		find_library(HYDRASDR_LIBRARY NAMES hydrasdr
			PATHS /usr/local/lib /usr/lib)
		if(HYDRASDR_INCLUDE_DIR AND HYDRASDR_LIBRARY)
			set(HYDRASDR_USE_LOCAL ON)
			message(STATUS "Using system libhydrasdr:")
			message(STATUS "  Header:  ${HYDRASDR_INCLUDE_DIR}")
			message(STATUS "  Library: ${HYDRASDR_LIBRARY}")
		endif()
	endif()
endif()

# 3. FetchContent from official GitHub repo
if(NOT HydraSDR_FOUND AND NOT HYDRASDR_USE_LOCAL)
	set(HYDRASDR_USE_FETCH ON)
	message(STATUS "libhydrasdr not found locally - cloning from GitHub...")

	# Git tag/branch to checkout (override with -DHYDRASDR_GIT_TAG=xxx)
	set(HYDRASDR_GIT_TAG "main" CACHE STRING
		"Git tag/branch/commit of hydrasdr-host to checkout")

	FetchContent_Declare(
		hydrasdr
		GIT_REPOSITORY https://github.com/hydrasdr/hydrasdr-host.git
		GIT_TAG        ${HYDRASDR_GIT_TAG}
		GIT_SHALLOW    ON
	)

	# Build shared library, don't build the tools
	set(BUILD_TOOLS OFF CACHE BOOL "" FORCE)
	set(BUILD_TESTING OFF CACHE BOOL "" FORCE)
	set(BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)

	FetchContent_MakeAvailable(hydrasdr)

	# After FetchContent, the hydrasdr target is available
	message(STATUS "libhydrasdr cloned from: https://github.com/hydrasdr/hydrasdr-host.git (${HYDRASDR_GIT_TAG})")
endif()

# ============================================================
# Build hydrasdr_jni shared library
# ============================================================
add_library(hydrasdr_jni SHARED hydrasdr_jni.c)

target_include_directories(hydrasdr_jni PRIVATE
	${JNI_INCLUDE_DIRS}
)

if(HYDRASDR_USE_FETCH)
	# FetchContent: link against the built target directly
	target_link_libraries(hydrasdr_jni PRIVATE hydrasdr)
	target_include_directories(hydrasdr_jni PRIVATE
		${hydrasdr_SOURCE_DIR}/libhydrasdr/src
	)
elseif(HydraSDR_FOUND)
	# System cmake package
	target_link_libraries(hydrasdr_jni PRIVATE HydraSDR::hydrasdr)
else()
	# Local manual paths
	target_include_directories(hydrasdr_jni PRIVATE ${HYDRASDR_INCLUDE_DIR})
	target_link_libraries(hydrasdr_jni PRIVATE ${HYDRASDR_LIBRARY})
endif()

# Platform-specific output naming
if(WIN32)
	set_target_properties(hydrasdr_jni PROPERTIES
		PREFIX ""
		SUFFIX ".dll"
	)
elseif(APPLE)
	set_target_properties(hydrasdr_jni PROPERTIES
		PREFIX "lib"
		SUFFIX ".dylib"
	)
else()
	set_target_properties(hydrasdr_jni PROPERTIES
		PREFIX "lib"
		SUFFIX ".so"
	)
endif()

# ============================================================
# Post-build: copy runtime dependency DLLs next to hydrasdr_jni
# ============================================================
if(WIN32)
	if(HYDRASDR_USE_FETCH)
		# FetchContent build: ask CMake where the hydrasdr target's DLL actually
		# lives (robust across BUILD_TOOLS=ON/OFF and any subdirectory layout
		# changes upstream). $<TARGET_FILE:hydrasdr> is resolved at build time.
		add_custom_command(TARGET hydrasdr_jni POST_BUILD
			COMMAND ${CMAKE_COMMAND} -E copy_if_different
				"$<TARGET_FILE:hydrasdr>"
				$<TARGET_FILE_DIR:hydrasdr_jni>
			COMMENT "Copying libhydrasdr.dll to build output"
		)
	elseif(HYDRASDR_DIR)
		# Local build: find and copy DLLs
		find_file(HYDRASDR_DLL
			NAMES libhydrasdr.dll hydrasdr.dll
			PATHS
				${HYDRASDR_DIR}/build/hydrasdr-tools/src
				${HYDRASDR_DIR}/build/libhydrasdr/src
				${HYDRASDR_DIR}/build/bin
				${HYDRASDR_DIR}/build
				${HYDRASDR_DIR}/bin
			NO_DEFAULT_PATH
		)

		if(HYDRASDR_DLL)
			message(STATUS "Will copy: ${HYDRASDR_DLL}")
			add_custom_command(TARGET hydrasdr_jni POST_BUILD
				COMMAND ${CMAKE_COMMAND} -E copy_if_different
					"${HYDRASDR_DLL}"
					$<TARGET_FILE_DIR:hydrasdr_jni>
				COMMENT "Copying libhydrasdr.dll to build output"
			)
		else()
			message(WARNING "libhydrasdr.dll not found - copy it manually next to hydrasdr_jni.dll")
		endif()
	endif()

	# Find libusb-1.0.dll from FetchContent deps or known paths.
	# Search order covers: vcpkg (GitHub Windows CI), MSYS2/MinGW (local dev),
	# and FetchContent build trees.
	if(HYDRASDR_USE_FETCH)
		find_file(LIBUSB_DLL NAMES libusb-1.0.dll
			PATHS
				${hydrasdr_BINARY_DIR}
				${CMAKE_BINARY_DIR}/_deps
				$ENV{MSYSTEM_PREFIX}/bin
				$ENV{VCPKG_INSTALLATION_ROOT}/installed/x64-windows/bin
				C:/vcpkg/installed/x64-windows/bin
			NO_DEFAULT_PATH
		)
	else()
		find_file(LIBUSB_DLL NAMES libusb-1.0.dll
			PATHS
				${HYDRASDR_DIR}/build/hydrasdr-tools/src
				${HYDRASDR_DIR}/build/bin
				${HYDRASDR_DIR}/build
				$ENV{MSYSTEM_PREFIX}/bin
				$ENV{VCPKG_INSTALLATION_ROOT}/installed/x64-windows/bin
				C:/vcpkg/installed/x64-windows/bin
			NO_DEFAULT_PATH
		)
	endif()

	if(LIBUSB_DLL)
		message(STATUS "Will copy: ${LIBUSB_DLL}")
		add_custom_command(TARGET hydrasdr_jni POST_BUILD
			COMMAND ${CMAKE_COMMAND} -E copy_if_different
				"${LIBUSB_DLL}"
				$<TARGET_FILE_DIR:hydrasdr_jni>
			COMMENT "Copying libusb-1.0.dll to build output"
		)
	else()
		message(WARNING "libusb-1.0.dll not found - copy it manually next to hydrasdr_jni.dll")
	endif()
endif()

# ============================================================
# Post-build: macOS dylib bundling
# ============================================================
# Bundle libhydrasdr.dylib next to libhydrasdr_jni.dylib so the runtime image
# is self-contained on macOS (no need for users to download libhydrasdr from
# the upstream release nightly). libusb is intentionally NOT bundled — it is
# expected to be installed via Homebrew (`brew install libusb`), matching the
# convention used by upstream hydrasdr-host-tools-macOS releases.
#
# macOS uses install names baked into the binary, so we must rewrite the
# JNI bridge's reference to libhydrasdr to use @loader_path. libhydrasdr's
# own reference to libusb is left as-is (will resolve via the system /
# Homebrew install).
#
# IMPORTANT: this code path has not been validated on a real Mac at the time
# of writing. The logic mirrors what upstream hydrasdr-host-tools does for its
# own macOS release archive.
if(APPLE AND HYDRASDR_USE_FETCH)
	# Macros / paths used in the post-build script:
	#   $<TARGET_FILE:hydrasdr>     -> libhydrasdr.<full version>.dylib (real file)
	#   $<TARGET_SONAME_FILE:hydrasdr> -> libhydrasdr.<SOVERSION>.dylib (the symlink
	#                                     name embedded in the install-name of
	#                                     anything linking against libhydrasdr)
	# We copy the real file AND the entire symlink chain (libhydrasdr.dylib ->
	# libhydrasdr.1.dylib -> libhydrasdr.1.1.1.dylib) so that whatever name the
	# dynamic linker resolves at runtime, it finds the file. cp -P preserves
	# symlinks (does not dereference them).
	add_custom_command(TARGET hydrasdr_jni POST_BUILD
		COMMAND bash -c "set -e; \
			SRC_DIR=\"$(dirname $<TARGET_FILE:hydrasdr>)\"; \
			DST_DIR='$<TARGET_FILE_DIR:hydrasdr_jni>'; \
			echo \"Copying libhydrasdr*.dylib (real + symlinks) from $SRC_DIR\"; \
			cp -P \"$SRC_DIR\"/libhydrasdr*.dylib \"$DST_DIR/\""
		VERBATIM
		COMMENT "Copying libhydrasdr dylib + symlink chain"
	)

	# Rewrite install names so libhydrasdr_jni.dylib finds libhydrasdr next to
	# itself instead of at the original FetchContent build-tree path.
	#
	# libusb references inside libhydrasdr are LEFT ALONE on purpose: the
	# convention used by upstream hydrasdr-host-tools-macOS releases is that
	# libusb is a system / Homebrew dependency (`brew install libusb`), not
	# something we ship. Mirroring that here.
	#
	# The script enumerates libhydrasdr_jni.dylib's actual linked deps via
	# otool and rewrites whatever path it finds (rather than hardcoding the
	# original install-name strings, which can vary across CMake versions).
	add_custom_command(TARGET hydrasdr_jni POST_BUILD
		COMMAND bash -c "set -e; \
			BINDIR='$<TARGET_FILE_DIR:hydrasdr_jni>'; \
			JNI_LIB='$<TARGET_FILE:hydrasdr_jni>'; \
			HYDRASDR_REAL_NAME='$<TARGET_FILE_NAME:hydrasdr>'; \
			HYDRASDR_REAL_LIB=\"$BINDIR/$HYDRASDR_REAL_NAME\"; \
			echo 'Fixing libhydrasdr install names for self-contained macOS bundle...'; \
			echo \"  JNI lib:     $JNI_LIB\"; \
			echo \"  HydraSDR lib: $HYDRASDR_REAL_LIB\"; \
			for old in $(otool -L \"$JNI_LIB\" | awk 'NR>1 && /libhydrasdr/ {print $1}'); do \
				new=\"@loader_path/$(basename $old)\"; \
				echo \"  install_name_tool -change $old $new\"; \
				install_name_tool -change \"$old\" \"$new\" \"$JNI_LIB\"; \
			done; \
			install_name_tool -id \"@loader_path/$HYDRASDR_REAL_NAME\" \"$HYDRASDR_REAL_LIB\"; \
			codesign --force --sign - \"$HYDRASDR_REAL_LIB\" \"$JNI_LIB\"; \
			echo 'Final dependencies:'; \
			otool -L \"$JNI_LIB\" \"$HYDRASDR_REAL_LIB\""
		VERBATIM
		COMMENT "Rewriting libhydrasdr install name + ad-hoc resigning"
	)
endif()

# ============================================================
# Install
# ============================================================
install(TARGETS hydrasdr_jni
	LIBRARY DESTINATION lib
	RUNTIME DESTINATION bin
)

if(WIN32)
	if(HYDRASDR_USE_FETCH)
		install(FILES "$<TARGET_FILE:hydrasdr>" DESTINATION bin)
	elseif(HYDRASDR_DLL)
		install(FILES "${HYDRASDR_DLL}" DESTINATION bin)
	endif()
	if(LIBUSB_DLL)
		install(FILES "${LIBUSB_DLL}" DESTINATION bin)
	endif()
endif()
