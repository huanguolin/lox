src_DIR 	:= src
build_DIR 	:= build

project_SRCS := $(wildcard $(src_DIR)/*.c)
project_raw_OBJS := $(patsubst %.c,%.o,$(project_SRCS))
project_OBJS := $(patsubst $(src_DIR)/%,$(build_DIR)/%,$(project_raw_OBJS))

project_INCLUDE_DIRS    += /Library/Developer/CommandLineTools/usr/include
project_LIBRARY_DIRS    += /Library/Developer/CommandLineTools/usr/lib
LDFLAGS                 += -Wl


.PHONY: all app clean copy $(project_OBJS) $(project_raw_OBJS)

all: app
	@- mkdir -p $(build_DIR)

app: $(project_OBJS)
	$(LINK.cc) $^ -o $@ $(LDFLAGS)

$(project_OBJS): copy

copy: $(project_raw_OBJS)
	@- mv $(project_raw_OBJS) $(build_DIR)

$(project_raw_OBJS):
	$(CC) -g -o0 -c $(patsubst %.o,%.c,$@) -o $@

clean:
	rm -f app $(project_OBJS) $(src_DIR)/*.o
